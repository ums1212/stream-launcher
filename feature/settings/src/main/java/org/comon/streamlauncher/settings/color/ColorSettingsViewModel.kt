package org.comon.streamlauncher.settings.color

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ColorSettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
) : BaseViewModel<ColorSettingsState, ColorSettingsIntent, ColorSettingsSideEffect>(ColorSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState { copy(colorPresetIndex = settings.colorPresetIndex) }
            }
        }
    }

    override fun handleIntent(intent: ColorSettingsIntent) {
        when (intent) {
            is ColorSettingsIntent.ChangeAccentColor -> changeAccentColor(intent.presetIndex)
        }
    }

    private fun changeAccentColor(presetIndex: Int) {
        updateState { copy(colorPresetIndex = presetIndex) }
        viewModelScope.launch {
            saveColorPresetUseCase(presetIndex)
        }
    }
}
