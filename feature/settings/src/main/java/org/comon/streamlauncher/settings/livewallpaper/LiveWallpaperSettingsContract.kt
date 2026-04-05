package org.comon.streamlauncher.settings.livewallpaper

import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class LiveWallpaperSettingsState(
    val liveWallpapers: List<LiveWallpaper> = emptyList(),
    val selectedOrientationTab: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    val selectedLiveWallpaperUri: String? = null,
    val selectedLiveWallpaperId: Int? = null,
    val selectedLiveWallpaperLandscapeUri: String? = null,
    val selectedLiveWallpaperLandscapeId: Int? = null,
    val activePortraitWallpaperId: Int? = null,
    val activeLandscapeWallpaperId: Int? = null,
    val isLiveWallpaperServiceActive: Boolean = false,
) : UiState

sealed interface LiveWallpaperSettingsIntent : UiIntent {
    data object CheckActiveWallpaper : LiveWallpaperSettingsIntent
    data class LoadLiveWallpaperFile(val uri: String) : LiveWallpaperSettingsIntent
    data class CreateLiveWallpaper(val name: String) : LiveWallpaperSettingsIntent
    data class SelectLiveWallpaper(val id: Int, val uri: String) : LiveWallpaperSettingsIntent
    data class SetActiveLiveWallpaper(
        val id: Int,
        val uri: String,
        val orientation: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    ) : LiveWallpaperSettingsIntent
    data class DeleteLiveWallpaper(val id: Int) : LiveWallpaperSettingsIntent
    data class ClearActiveLiveWallpaper(
        val orientation: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    ) : LiveWallpaperSettingsIntent
    data class SwitchOrientationTab(val orientation: WallpaperOrientation) : LiveWallpaperSettingsIntent
    /**
     * 피커에서 돌아온 뒤 가로 배경화면을 DataStore에 확정 저장한다.
     * id / uri 가 null 이면 가로 배경화면을 해제한다.
     */
    data class ConfirmLandscapeWallpaper(val id: Int?, val uri: String?) : LiveWallpaperSettingsIntent
}

sealed interface LiveWallpaperSettingsSideEffect : UiSideEffect {
    /**
     * 시스템 라이브 배경화면 피커를 실행한다.
     *
     * - [landscapeNewId] / [landscapeNewUri] 가 non-null → 가로 배경화면 설정 플로우.
     *   MainActivity는 피커 미리보기를 위해 portrait 파일을 [landscapeNewUri] 로 임시 교체하고
     *   startActivity 로 피커를 연 뒤, onResume 에서 가로 배경화면을 확정 적용한다.
     * - 둘 다 null → 세로 배경화면 설정 플로우 (기존 동작).
     */
    data class LaunchLiveWallpaperPicker(
        val landscapeNewId: Int? = null,
        val landscapeNewUri: String? = null,
    ) : LiveWallpaperSettingsSideEffect
    data object ReloadWallpaper : LiveWallpaperSettingsSideEffect
    data class ShowError(val message: String) : LiveWallpaperSettingsSideEffect
}
