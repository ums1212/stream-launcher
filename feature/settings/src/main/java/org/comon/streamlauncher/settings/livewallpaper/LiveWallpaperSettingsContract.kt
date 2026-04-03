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
}

sealed interface LiveWallpaperSettingsSideEffect : UiSideEffect {
    data object LaunchLiveWallpaperPicker : LiveWallpaperSettingsSideEffect
    data object ReloadWallpaper : LiveWallpaperSettingsSideEffect
    data class ShowError(val message: String) : LiveWallpaperSettingsSideEffect
    data object ShowNetworkError : LiveWallpaperSettingsSideEffect
}
