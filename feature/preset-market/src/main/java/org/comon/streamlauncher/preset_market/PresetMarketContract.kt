package org.comon.streamlauncher.preset_market

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class PresetMarketState(
    val currentUser: MarketUser? = null,
    val topDownloadPresets: List<MarketPreset> = emptyList(),
    val topLikePresets: List<MarketPreset> = emptyList(),
    val selectedTab: MarketTab = MarketTab.DOWNLOADS,
    val isLoading: Boolean = false,
    val error: String? = null,
) : UiState

enum class MarketTab { DOWNLOADS, LIKES }

sealed interface PresetMarketIntent : UiIntent {
    data object LoadTopPresets : PresetMarketIntent
    data class SelectTab(val tab: MarketTab) : PresetMarketIntent
    data class ClickPreset(val presetId: String) : PresetMarketIntent
    data class SignInWithGoogle(val idToken: String) : PresetMarketIntent
    data object SignOut : PresetMarketIntent
    data object NavigateToSearch : PresetMarketIntent
    data object NavigateToUserInfo : PresetMarketIntent
    data object DismissError : PresetMarketIntent
}

sealed interface PresetMarketSideEffect : UiSideEffect {
    data class NavigateToDetail(val presetId: String) : PresetMarketSideEffect
    data object NavigateToSearch : PresetMarketSideEffect
    data object NavigateToUserInfo : PresetMarketSideEffect
    data class ShowError(val message: String) : PresetMarketSideEffect
    data object RequireSignIn : PresetMarketSideEffect
    data object SignInSuccess : PresetMarketSideEffect
}
