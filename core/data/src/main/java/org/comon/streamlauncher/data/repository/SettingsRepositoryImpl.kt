package org.comon.streamlauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

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
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val colorPresetIndexKey = intPreferencesKey("color_preset_index")
    private val gridCellImagesKey = stringPreferencesKey("grid_cell_images")
    private val cellAssignmentsKey = stringPreferencesKey("cell_assignments")
    private val chzzkChannelIdKey = stringPreferencesKey("chzzk_channel_id")
    private val youtubeChannelIdKey = stringPreferencesKey("youtube_channel_id")
    private val rssUrlKey = stringPreferencesKey("rss_url")
    private val wallpaperImageKey = stringPreferencesKey("launcher_background_image")

    override fun getSettings(): Flow<LauncherSettings> =
        context.dataStore.data.map { prefs ->
            val colorPresetIndex = prefs[colorPresetIndexKey] ?: 0
            val imagesJson = prefs[gridCellImagesKey]
            val gridCellImages = parseGridCellImages(imagesJson)
            val assignmentsJson = prefs[cellAssignmentsKey]
            val cellAssignments = parseCellAssignments(assignmentsJson)
            val chzzkChannelId = prefs[chzzkChannelIdKey] ?: "d2fb83a5db130bf4d273c981b82ca41f"
            val youtubeChannelId = prefs[youtubeChannelIdKey] ?: ""
            val rssUrl = prefs[rssUrlKey] ?: ""
            val wallpaperImage = prefs[wallpaperImageKey]
            LauncherSettings(
                colorPresetIndex = colorPresetIndex,
                gridCellImages = gridCellImages,
                cellAssignments = cellAssignments,
                chzzkChannelId = chzzkChannelId,
                youtubeChannelId = youtubeChannelId,
                rssUrl = rssUrl,
                wallpaperImage = wallpaperImage,
            )
        }

    override suspend fun setColorPresetIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[colorPresetIndexKey] = index
        }
    }

    override suspend fun setGridCellImage(cell: GridCell, idle: String?, expanded: String?) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[gridCellImagesKey]
            val current = parseGridCellImageDtos(currentJson).toMutableList()
            val cellOrdinal = cell.ordinal
            current.removeAll { it.cell == cellOrdinal }
            current.add(GridCellImageDto(cell = cellOrdinal, idle = idle, expanded = expanded))
            prefs[gridCellImagesKey] = Json.encodeToString(current)
        }
    }

    override suspend fun setChzzkChannelId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[chzzkChannelIdKey] = id
        }
    }

    override suspend fun setYoutubeChannelId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[youtubeChannelIdKey] = id
        }
    }

    override suspend fun setRssUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[rssUrlKey] = url
        }
    }

    override suspend fun setWallpaperImage(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[wallpaperImageKey] = uri
            } else {
                prefs.remove(wallpaperImageKey)
            }
        }
    }

    override suspend fun setCellAssignment(cell: GridCell, packageNames: List<String>) {
        context.dataStore.edit { prefs ->
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            emptyList()
        }
    }
}
