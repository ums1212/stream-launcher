package org.comon.streamlauncher.data.repository

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
@Serializable
private data class GridCellImageDto(
    val cell: Int,
    val idle: String? = null,
    val expanded: String? = null,
)

@Serializable
private data class CellAssignmentDto(
    val cell: Int,
    val packages: List<String>,
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    /** :wallpaper 프로세스가 읽는 URI 파일 (filesDir 공유) */
    private val wallpaperUriFile get() = File(context.filesDir, "live_wallpaper_uri.txt")
    private val wallpaperLandscapeUriFile get() = File(context.filesDir, "live_wallpaper_uri_landscape.txt")

    /**
     * VideoLiveWallpaperService 브로드캐스트 액션.
     * core:data 에서 app 모듈 직접 의존 없이 사용하기 위해 문자열로 유지.
     */
    private val actionReloadUri = "org.comon.streamlauncher.action.RELOAD_WALLPAPER_URI"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 앱 시작 시 DataStore ↔ URI 파일 동기화.
        // Activity가 가로 피커 플로우 도중 강제 종료되면 세로 파일에 가로 URI가 남을 수 있다.
        // DataStore 가 source-of-truth 이므로 시작 시 파일을 덮어써서 불일치를 복구한다.
        ioScope.launch {
            try {
                val prefs = dataStore.data.first()
                val portraitUri = prefs[liveWallpaperUriKey]
                val landscapeUri = prefs[liveWallpaperLandscapeUriKey]
                if (portraitUri != null) wallpaperUriFile.writeText(portraitUri)
                else if (wallpaperUriFile.exists()) wallpaperUriFile.delete()
                if (landscapeUri != null) wallpaperLandscapeUriFile.writeText(landscapeUri)
                else if (wallpaperLandscapeUriFile.exists()) wallpaperLandscapeUriFile.delete()
                // 동기화 후 서비스에 reload 통보
                context.sendBroadcast(
                    Intent(actionReloadUri).apply { `package` = context.packageName }
                )
            } catch (_: Exception) { /* 초기 동기화 실패 시 무시 */ }
        }
    }

    private val hasShownHomeSettingsOnFirstLaunchKey = booleanPreferencesKey("has_shown_home_settings_on_first_launch")
    private val colorPresetIndexKey = intPreferencesKey("color_preset_index")
    private val gridCellImagesKey = stringPreferencesKey("grid_cell_images")
    private val cellAssignmentsKey = stringPreferencesKey("cell_assignments")
    private val chzzkChannelIdKey = stringPreferencesKey("chzzk_channel_id")
    private val youtubeChannelIdKey = stringPreferencesKey("youtube_channel_id")
    private val wallpaperImageKey = stringPreferencesKey("launcher_background_image")
    private val appDrawerGridColumnsKey = intPreferencesKey("app_drawer_grid_columns")
    private val appDrawerGridRowsKey = intPreferencesKey("app_drawer_grid_rows")
    private val appDrawerIconSizeRatioKey = floatPreferencesKey("app_drawer_icon_size_ratio")
    private val lastShownNoticeVersionKey = stringPreferencesKey("last_shown_notice_version")
    private val liveWallpaperIdKey = intPreferencesKey("live_wallpaper_id")
    private val liveWallpaperUriKey = stringPreferencesKey("live_wallpaper_uri")
    private val liveWallpaperLandscapeIdKey = intPreferencesKey("live_wallpaper_landscape_id")
    private val liveWallpaperLandscapeUriKey = stringPreferencesKey("live_wallpaper_landscape_uri")
    private val staticWallpaperPortraitUriKey = stringPreferencesKey("static_wallpaper_portrait_uri")
    private val staticWallpaperLandscapeUriKey = stringPreferencesKey("static_wallpaper_landscape_uri")

    override fun getSettings(): Flow<LauncherSettings> =
        dataStore.data.map { prefs ->
            val colorPresetIndex = prefs[colorPresetIndexKey] ?: 0
            val imagesJson = prefs[gridCellImagesKey]
            val gridCellImages = parseGridCellImages(imagesJson)
            val assignmentsJson = prefs[cellAssignmentsKey]
            val cellAssignments = parseCellAssignments(assignmentsJson)
            val chzzkChannelId = prefs[chzzkChannelIdKey] ?: ""
            val youtubeChannelId = prefs[youtubeChannelIdKey] ?: ""
            val wallpaperImage = prefs[wallpaperImageKey]
            val appDrawerGridColumns = prefs[appDrawerGridColumnsKey] ?: 4
            val appDrawerGridRows = prefs[appDrawerGridRowsKey] ?: 6
            val appDrawerIconSizeRatio = prefs[appDrawerIconSizeRatioKey] ?: 1.0f
            val liveWallpaperId = prefs[liveWallpaperIdKey]
            val liveWallpaperUri = prefs[liveWallpaperUriKey]
            val liveWallpaperLandscapeId = prefs[liveWallpaperLandscapeIdKey]
            val liveWallpaperLandscapeUri = prefs[liveWallpaperLandscapeUriKey]
            val staticWallpaperPortraitUri = prefs[staticWallpaperPortraitUriKey]
            val staticWallpaperLandscapeUri = prefs[staticWallpaperLandscapeUriKey]
            LauncherSettings(
                colorPresetIndex = colorPresetIndex,
                gridCellImages = gridCellImages,
                cellAssignments = cellAssignments,
                chzzkChannelId = chzzkChannelId,
                youtubeChannelId = youtubeChannelId,
                wallpaperImage = wallpaperImage,
                appDrawerGridColumns = appDrawerGridColumns,
                appDrawerGridRows = appDrawerGridRows,
                appDrawerIconSizeRatio = appDrawerIconSizeRatio,
                liveWallpaperId = liveWallpaperId,
                liveWallpaperUri = liveWallpaperUri,
                liveWallpaperLandscapeId = liveWallpaperLandscapeId,
                liveWallpaperLandscapeUri = liveWallpaperLandscapeUri,
                staticWallpaperPortraitUri = staticWallpaperPortraitUri,
                staticWallpaperLandscapeUri = staticWallpaperLandscapeUri,
            )
        }

    override suspend fun hasShownHomeSettingsOnFirstLaunch(): Boolean =
        dataStore.data.first()[hasShownHomeSettingsOnFirstLaunchKey] ?: false

    override suspend fun setHasShownHomeSettingsOnFirstLaunch() {
        dataStore.edit { prefs ->
            prefs[hasShownHomeSettingsOnFirstLaunchKey] = true
        }
    }

    override suspend fun setColorPresetIndex(index: Int) {
        dataStore.edit { prefs ->
            prefs[colorPresetIndexKey] = index
        }
    }

    override suspend fun setGridCellImage(cell: GridCell, idle: String?, expanded: String?) {
        dataStore.edit { prefs ->
            val currentJson = prefs[gridCellImagesKey]
            val current = parseGridCellImageDtos(currentJson).toMutableList()
            val cellOrdinal = cell.ordinal
            current.removeAll { it.cell == cellOrdinal }
            current.add(GridCellImageDto(cell = cellOrdinal, idle = idle, expanded = expanded))
            prefs[gridCellImagesKey] = Json.encodeToString(current)
        }
    }

    override suspend fun setChzzkChannelId(id: String) {
        dataStore.edit { prefs ->
            prefs[chzzkChannelIdKey] = id
        }
    }

    override suspend fun setYoutubeChannelId(id: String) {
        dataStore.edit { prefs ->
            prefs[youtubeChannelIdKey] = id
        }
    }

    override suspend fun setWallpaperImage(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) {
                prefs[wallpaperImageKey] = uri
            } else {
                prefs.remove(wallpaperImageKey)
            }
        }
    }

    override suspend fun setCellAssignment(cell: GridCell, packageNames: List<String>) {
        dataStore.edit { prefs ->
            val currentJson = prefs[cellAssignmentsKey]
            val current = parseCellAssignmentDtos(currentJson).toMutableList()
            val cellOrdinal = cell.ordinal
            current.removeAll { it.cell == cellOrdinal }
            if (packageNames.isNotEmpty()) {
                current.add(CellAssignmentDto(cell = cellOrdinal, packages = packageNames))
            }
            prefs[cellAssignmentsKey] = Json.encodeToString(current)
        }
    }

    override suspend fun setAppDrawerSettings(columns: Int, rows: Int, iconSizeRatio: Float) {
        dataStore.edit { prefs ->
            prefs[appDrawerGridColumnsKey] = columns
            prefs[appDrawerGridRowsKey] = rows
            prefs[appDrawerIconSizeRatioKey] = iconSizeRatio
        }
    }

    override suspend fun getLastShownNoticeVersion(): String? =
        dataStore.data.first()[lastShownNoticeVersionKey]

    override suspend fun setLastShownNoticeVersion(version: String) {
        dataStore.edit { prefs ->
            prefs[lastShownNoticeVersionKey] = version
        }
    }

    override fun getLiveWallpaperUri(): Flow<String?> =
        dataStore.data.map { prefs -> prefs[liveWallpaperUriKey] }

    override suspend fun setLiveWallpaper(id: Int?, uri: String?, orientation: WallpaperOrientation) {
        if (orientation == WallpaperOrientation.LANDSCAPE) {
            dataStore.edit { prefs ->
                if (id != null) prefs[liveWallpaperLandscapeIdKey] = id else prefs.remove(liveWallpaperLandscapeIdKey)
                if (uri != null) prefs[liveWallpaperLandscapeUriKey] = uri else prefs.remove(liveWallpaperLandscapeUriKey)
            }
            withContext(Dispatchers.IO) {
                if (uri != null) wallpaperLandscapeUriFile.writeText(uri)
                else if (wallpaperLandscapeUriFile.exists()) wallpaperLandscapeUriFile.delete()
                // clear 포함 모든 경우에 서비스에 reload 통보
                context.sendBroadcast(
                    Intent(actionReloadUri).apply { `package` = context.packageName }
                )
            }
        } else {
            dataStore.edit { prefs ->
                if (id != null) prefs[liveWallpaperIdKey] = id else prefs.remove(liveWallpaperIdKey)
                if (uri != null) prefs[liveWallpaperUriKey] = uri else prefs.remove(liveWallpaperUriKey)
            }
            withContext(Dispatchers.IO) {
                if (uri != null) wallpaperUriFile.writeText(uri)
                else if (wallpaperUriFile.exists()) wallpaperUriFile.delete()
                // clear 포함 모든 경우에 서비스에 reload 통보
                context.sendBroadcast(
                    Intent(actionReloadUri).apply { `package` = context.packageName }
                )
            }
        }
    }

    override suspend fun setStaticWallpaper(uri: String?, orientation: WallpaperOrientation) {
        dataStore.edit { prefs ->
            if (orientation == WallpaperOrientation.LANDSCAPE) {
                if (uri != null) prefs[staticWallpaperLandscapeUriKey] = uri
                else prefs.remove(staticWallpaperLandscapeUriKey)
            } else {
                if (uri != null) prefs[staticWallpaperPortraitUriKey] = uri
                else prefs.remove(staticWallpaperPortraitUriKey)
            }
        }
    }

    private fun parseCellAssignments(json: String?): Map<GridCell, List<String>> {
        val dtos = parseCellAssignmentDtos(json)
        val result = mutableMapOf<GridCell, List<String>>()
        dtos.forEach { dto ->
            val cell = GridCell.entries.getOrNull(dto.cell) ?: return@forEach
            result[cell] = dto.packages
        }
        return result
    }

    private fun parseCellAssignmentDtos(json: String?): List<CellAssignmentDto> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseGridCellImages(json: String?): Map<GridCell, GridCellImage> {
        val dtos = parseGridCellImageDtos(json)
        val result = GridCell.entries.associateWith { GridCellImage(it) }.toMutableMap()
        dtos.forEach { dto ->
            val cell = GridCell.entries.getOrNull(dto.cell) ?: return@forEach
            result[cell] = GridCellImage(
                cell = cell,
                idleImageUri = dto.idle,
                expandedImageUri = dto.expanded,
            )
        }
        return result
    }

    private fun parseGridCellImageDtos(json: String?): List<GridCellImageDto> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
