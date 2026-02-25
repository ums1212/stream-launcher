package org.comon.streamlauncher.launcher

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.usecase.GetChannelProfileUseCase
import org.comon.streamlauncher.domain.usecase.GetIntegratedFeedUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.GetChzzkLiveStatusUseCase
import org.comon.streamlauncher.domain.usecase.GetYoutubeLiveStatusUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

private const val MIN_REFRESH_INTERVAL_MS = 60_000L

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val getChzzkLiveStatusUseCase: GetChzzkLiveStatusUseCase,
    private val getYoutubeLiveStatusUseCase: GetYoutubeLiveStatusUseCase,
    private val getIntegratedFeedUseCase: GetIntegratedFeedUseCase,
    private val getChannelProfileUseCase: GetChannelProfileUseCase,
) : BaseViewModel<FeedState, FeedIntent, FeedSideEffect>(FeedState()) {

    private var loadJob: Job? = null
    private var lastRefreshMillis: Long = 0L

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                val prev = currentState
                val isChanged = prev.chzzkChannelId != settings.chzzkChannelId ||
                        prev.youtubeChannelId != settings.youtubeChannelId ||
                        prev.rssUrl != settings.rssUrl

                updateState {
                    copy(
                        chzzkChannelId = settings.chzzkChannelId,
                        rssUrl = settings.rssUrl,
                        youtubeChannelId = settings.youtubeChannelId,
                    )
                }
                refresh(force = lastRefreshMillis == 0L || isChanged)
            }
        }
    }

    override fun handleIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.Refresh -> refresh(force = false)
            is FeedIntent.ClickFeedItem -> openFeedItemUrl(intent.item)
            is FeedIntent.ClickLiveStatus -> openLiveStream()
            is FeedIntent.ClickOfflineStatus -> openChzzkChannelPage()
            is FeedIntent.ClickChannelProfile -> openChannelPage()
        }
    }

    private fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshMillis < MIN_REFRESH_INTERVAL_MS) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }

            val channelId = currentState.chzzkChannelId
            val rssUrl = currentState.rssUrl
            val youtubeChannelId = currentState.youtubeChannelId

            val liveJob = launch {
                if (channelId.isEmpty()) {
                    updateState { copy(liveStatus = null) }
                    return@launch
                }
                try {
                    getChzzkLiveStatusUseCase(channelId).collect { result ->
                        result.fold(
                            onSuccess = { status -> updateState { copy(liveStatus = status) } },
                            onFailure = { /* 라이브 상태 오류는 조용히 무시 */ },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { /* ignore */ }
            }

            val feedJob = launch {
                try {
                    getIntegratedFeedUseCase(rssUrl, youtubeChannelId).collect { result ->
                        result.fold(
                            onSuccess = { items ->
                                updateState { copy(feedItems = items, isLoading = false) }
                                lastRefreshMillis = System.currentTimeMillis()
                            },
                            onFailure = { e ->
                                updateState { copy(isLoading = false, errorMessage = e.message) }
                                sendEffect(FeedSideEffect.ShowError(e.message ?: "피드를 불러오는 중 오류가 발생했습니다."))
                            },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    updateState { copy(isLoading = false, errorMessage = e.message) }
                }
            }

            val profileJob = launch {
                if (youtubeChannelId.isEmpty()) {
                    updateState { copy(channelProfile = null) }
                    return@launch
                }
                try {
                    getChannelProfileUseCase(youtubeChannelId).collect { result ->
                        result.onSuccess { profile ->
                            updateState { copy(channelProfile = profile) }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { /* 프로필 fetch 실패는 silent */ }
            }

            val youtubeLiveJob = launch {
                if (youtubeChannelId.isEmpty()) {
                    updateState { copy(youtubeLiveStatus = null) }
                    return@launch
                }
                try {
                    getYoutubeLiveStatusUseCase(youtubeChannelId).collect { result ->
                        result.fold(
                            onSuccess = { status -> updateState { copy(youtubeLiveStatus = status) } },
                            onFailure = { /* 라이브 상태 오류 무시 */ },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { /* ignore */ }
            }

            liveJob.join()
            youtubeLiveJob.join()
            feedJob.join()
            profileJob.join()
        }
    }

    private fun openFeedItemUrl(item: FeedItem) {
        val url = when (item) {
            is FeedItem.NoticeItem -> item.link
            is FeedItem.VideoItem -> item.videoLink
        }
        if (url.isNotEmpty()) {
            sendEffect(FeedSideEffect.OpenUrl(url))
        }
    }

    private fun openLiveStream() {
        val channelId = currentState.chzzkChannelId
        if (channelId.isNotEmpty()) {
            sendEffect(FeedSideEffect.OpenUrl("https://chzzk.naver.com/live/$channelId"))
        }
    }

    private fun openChzzkChannelPage() {
        val channelId = currentState.chzzkChannelId
        if (channelId.isNotEmpty()) {
            sendEffect(FeedSideEffect.OpenUrl("https://chzzk.naver.com/$channelId"))
        }
    }

    private fun openChannelPage() {
        val channelId = currentState.channelProfile?.channelId
            ?: currentState.youtubeChannelId
        if (channelId.isNotEmpty()) {
            sendEffect(FeedSideEffect.OpenUrl("https://www.youtube.com/channel/$channelId"))
        }
    }
}
