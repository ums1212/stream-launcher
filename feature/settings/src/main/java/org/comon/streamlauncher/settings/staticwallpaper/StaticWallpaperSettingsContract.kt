package org.comon.streamlauncher.settings.staticwallpaper

import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class StaticWallpaperSettingsState(
    val staticWallpaperPortraitUri: String? = null,
    val staticWallpaperLandscapeUri: String? = null,
    val selectedStaticWallpaperTab: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
) : UiState

sealed interface StaticWallpaperSettingsIntent : UiIntent {
    data class SetStaticWallpaper(
        val uri: String,
        val orientation: WallpaperOrientation,
        val isCurrentLandscape: Boolean,
    ) : StaticWallpaperSettingsIntent
    data class ClearStaticWallpaper(val orientation: WallpaperOrientation) : StaticWallpaperSettingsIntent
    data class SwitchStaticWallpaperTab(val orientation: WallpaperOrientation) : StaticWallpaperSettingsIntent
}

sealed interface StaticWallpaperSettingsSideEffect : UiSideEffect {
    data class ShowError(val message: String) : StaticWallpaperSettingsSideEffect
    data object ShowNetworkError : StaticWallpaperSettingsSideEffect
}
