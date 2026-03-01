package org.comon.streamlauncher.domain.util

interface WallpaperHelper {
    suspend fun copyWallpaperFromUri(sourceUri: String, presetId: Long): String?
    suspend fun setWallpaperFromPreset(filePath: String): Boolean
    suspend fun deletePresetWallpaper(filePath: String)
}
