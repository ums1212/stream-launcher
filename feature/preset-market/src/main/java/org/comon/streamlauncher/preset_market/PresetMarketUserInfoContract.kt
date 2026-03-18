package org.comon.streamlauncher.preset_market

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class PresetMarketUserInfoState(
    val user: MarketUser? = null,
    val presets: List<MarketPreset> = emptyList(),
    val isLoading: Boolean = false,
) : UiState

sealed interface PresetMarketUserInfoIntent : UiIntent {
    data object LoadUserPresets : PresetMarketUserInfoIntent
    data object SignOut : PresetMarketUserInfoIntent
    data class ClickPreset(val presetId: String) : PresetMarketUserInfoIntent
}

sealed interface PresetMarketUserInfoSideEffect : UiSideEffect {
    data class NavigateToDetail(val presetId: String) : PresetMarketUserInfoSideEffect
    data class ShowError(val message: String) : PresetMarketUserInfoSideEffect
    data object SignedOut : PresetMarketUserInfoSideEffect
}
