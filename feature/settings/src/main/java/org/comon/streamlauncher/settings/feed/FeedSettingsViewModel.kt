package org.comon.streamlauncher.settings.feed

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FeedSettingsViewModel @Inject constructor(
    private val connectivityChecker: NetworkConnectivityChecker,
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
) : BaseViewModel<FeedSettingsState, FeedSettingsIntent, FeedSettingsSideEffect>(FeedSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        chzzkChannelId = settings.chzzkChannelId,
                        youtubeChannelId = settings.youtubeChannelId,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: FeedSettingsIntent) {
        when (intent) {
            is FeedSettingsIntent.SaveFeedSettings -> saveFeedSettings(
                intent.chzzkChannelId,
                intent.youtubeChannelId,
            )
        }
    }

    private fun saveFeedSettings(chzzkChannelId: String, youtubeChannelId: String) {
        updateState { copy(chzzkChannelId = chzzkChannelId, youtubeChannelId = youtubeChannelId) }
        viewModelScope.launch {
            runCatching {
                saveFeedSettingsUseCase(chzzkChannelId, youtubeChannelId)
            }.onFailure { error ->
                if (connectivityChecker.isUnavailable()) sendEffect(FeedSettingsSideEffect.ShowNetworkError)
                else sendEffect(FeedSettingsSideEffect.ShowError(error.getErrorMessage("피드 설정 저장")))
            }
        }
    }
}
