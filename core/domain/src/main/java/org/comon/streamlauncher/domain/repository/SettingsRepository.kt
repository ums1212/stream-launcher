package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.LauncherSettings

interface SettingsRepository {
    fun getSettings(): Flow<LauncherSettings>
    suspend fun hasShownHomeSettingsOnFirstLaunch(): Boolean
    suspend fun setHasShownHomeSettingsOnFirstLaunch()
    suspend fun setColorPresetIndex(index: Int)
    suspend fun setGridCellImage(cell: GridCell, idle: String?, expanded: String?)
    suspend fun setCellAssignment(cell: GridCell, packageNames: List<String>)
    suspend fun setChzzkChannelId(id: String)
    suspend fun setYoutubeChannelId(id: String)
    suspend fun setWallpaperImage(uri: String?)
    suspend fun setAppDrawerSettings(columns: Int, rows: Int, iconSizeRatio: Float)
    suspend fun getLastShownNoticeVersion(): String?
    suspend fun setLastShownNoticeVersion(version: String)
}
