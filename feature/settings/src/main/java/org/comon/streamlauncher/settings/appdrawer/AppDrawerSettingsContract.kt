package org.comon.streamlauncher.settings.appdrawer

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class AppDrawerSettingsState(
    val appDrawerGridColumns: Int = 4,
    val appDrawerGridRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,
) : UiState

sealed interface AppDrawerSettingsIntent : UiIntent {
    data class SaveAppDrawerSettings(
        val columns: Int,
        val rows: Int,
        val iconSizeRatio: Float,
    ) : AppDrawerSettingsIntent
}

sealed interface AppDrawerSettingsSideEffect : UiSideEffect {
    data class ShowError(val message: String) : AppDrawerSettingsSideEffect
    data object ShowNetworkError : AppDrawerSettingsSideEffect
}
