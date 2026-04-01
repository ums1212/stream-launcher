package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.repository.SettingsRepository
import org.comon.streamlauncher.domain.util.WallpaperHelper
import javax.inject.Inject

class SetStaticWallpaperUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wallpaperHelper: WallpaperHelper,
) {
    /**
     * uri가 null이면 해당 방향 배경화면을 초기화합니다.
     * 성공 시 저장된 파일 경로를 반환하고, 실패 시 null을 반환합니다.
     * WallpaperManager 적용은 호출부에서 현재 기기 방향을 확인 후 처리합니다.
     */
    suspend operator fun invoke(uri: String?, orientation: WallpaperOrientation): String? {
        if (uri == null) {
            settingsRepository.setStaticWallpaper(null, orientation)
            return ""
        }
        val isPortrait = orientation == WallpaperOrientation.PORTRAIT
        val filePath = wallpaperHelper.copyStaticWallpaperFromUri(uri, isPortrait) ?: return null
        settingsRepository.setStaticWallpaper(filePath, orientation)
        return filePath
    }
}
