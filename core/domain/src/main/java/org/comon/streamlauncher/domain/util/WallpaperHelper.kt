package org.comon.streamlauncher.domain.util

interface WallpaperHelper {
    suspend fun copyWallpaperFromUri(sourceUri: String, presetId: Long): String?
    suspend fun setWallpaperFromPreset(filePath: String): Boolean
    suspend fun deletePresetWallpaper(filePath: String)
    /** 현재 시스템 배경화면이 이 앱의 라이브 배경화면 서비스인지 반환 */
    fun isLiveWallpaperServiceActive(): Boolean
}
