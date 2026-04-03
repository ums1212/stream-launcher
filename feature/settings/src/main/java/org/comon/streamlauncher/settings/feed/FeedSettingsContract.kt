package org.comon.streamlauncher.settings.feed

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class FeedSettingsState(
    val chzzkChannelId: String = "",
    val youtubeChannelId: String = "",
) : UiState

sealed interface FeedSettingsIntent : UiIntent {
    data class SaveFeedSettings(
        val chzzkChannelId: String,
        val youtubeChannelId: String,
    ) : FeedSettingsIntent
}

sealed interface FeedSettingsSideEffect : UiSideEffect {
    data class ShowError(val message: String) : FeedSettingsSideEffect
}
