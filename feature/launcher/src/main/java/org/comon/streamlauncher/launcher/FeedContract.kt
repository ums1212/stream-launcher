package org.comon.streamlauncher.launcher

import org.comon.streamlauncher.domain.model.ChannelProfile
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class FeedState(
    val liveStatus: LiveStatus? = null,
    val youtubeLiveStatus: LiveStatus? = null,
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val chzzkChannelId: String = "",
    val youtubeChannelId: String = "",
    val channelProfile: ChannelProfile? = null,
) : UiState

sealed interface FeedIntent : UiIntent {
    data object Refresh : FeedIntent
    data class ClickFeedItem(val item: FeedItem) : FeedIntent
    data object ClickLiveStatus : FeedIntent
    data object ClickOfflineStatus : FeedIntent
    data object ClickChannelProfile : FeedIntent
}

sealed interface FeedSideEffect : UiSideEffect {
    data class OpenUrl(val url: String) : FeedSideEffect
    data class ShowError(val message: String) : FeedSideEffect
}
