package org.comon.streamlauncher.preset_market

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class MarketSearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) : UiState

sealed interface MarketSearchIntent : UiIntent {
    data class Search(val query: String) : MarketSearchIntent
    data class ClickPreset(val presetId: String) : MarketSearchIntent
}

sealed interface MarketSearchSideEffect : UiSideEffect {
    data class NavigateToDetail(val presetId: String) : MarketSearchSideEffect
}
