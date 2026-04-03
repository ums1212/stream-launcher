package org.comon.streamlauncher.settings.color

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class ColorSettingsState(
    val colorPresetIndex: Int = 0,
) : UiState

sealed interface ColorSettingsIntent : UiIntent {
    data class ChangeAccentColor(val presetIndex: Int) : ColorSettingsIntent
}

sealed interface ColorSettingsSideEffect : UiSideEffect
