package org.comon.streamlauncher.domain.util

interface WallpaperHelper {
    suspend fun copyWallpaperFromUri(sourceUri: String, presetId: Long): String?
    suspend fun setWallpaperFromPreset(filePath: String): Boolean
    suspend fun deletePresetWallpaper(filePath: String)
    /** 현재 시스템 배경화면이 이 앱의 라이브 배경화면 서비스인지 반환 */
    fun isLiveWallpaperServiceActive(): Boolean
    /** 갤러리 URI에서 정적 배경화면 파일을 내부 저장소에 복사 후 경로 반환 (세로/가로 구분) */
    suspend fun copyStaticWallpaperFromUri(sourceUri: String, isPortrait: Boolean): String?
}
