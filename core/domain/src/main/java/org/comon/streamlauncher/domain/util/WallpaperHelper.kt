package org.comon.streamlauncher.domain.util

interface WallpaperHelper {
    suspend fun saveCurrentWallpaperForPreset(presetId: Long): String?
    suspend fun setWallpaperFromPreset(filePath: String): Boolean
    suspend fun deletePresetWallpaper(filePath: String)
}
