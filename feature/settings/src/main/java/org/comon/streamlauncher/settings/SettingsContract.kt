package org.comon.streamlauncher.settings

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class SettingsState(
    val colorPresetIndex: Int = 0,
    val showNoticeDialog: Boolean = false,
    val staticWallpaperPortraitUri: String? = null,
    val staticWallpaperLandscapeUri: String? = null,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data object ShowNotice : SettingsIntent
    data object DismissNotice : SettingsIntent
    data object ResetTab : SettingsIntent
    data class ApplyStaticWallpaperForOrientation(val isLandscape: Boolean) : SettingsIntent
    data class SignInWithGoogle(val idToken: String) : SettingsIntent
}

sealed interface SettingsSideEffect : UiSideEffect {
    data object NavigateToMain : SettingsSideEffect
    data class ShowError(val message: String) : SettingsSideEffect
}
