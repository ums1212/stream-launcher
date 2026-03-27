package org.comon.streamlauncher.preset_market

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.paging.GetRecentPresetsPagerUseCase
import org.comon.streamlauncher.domain.usecase.GetTopDownloadPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetTopLikePresetsUseCase
import org.comon.streamlauncher.domain.usecase.ObserveAuthStateUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.usecase.SignOutUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.network.error.isNetworkDisconnected
import javax.inject.Inject

@HiltViewModel
class PresetMarketViewModel @Inject constructor(
    private val getTopDownloadPresetsUseCase: GetTopDownloadPresetsUseCase,
    private val getTopLikePresetsUseCase: GetTopLikePresetsUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getRecentPresetsPagerUseCase: GetRecentPresetsPagerUseCase,
) : BaseViewModel<PresetMarketState, PresetMarketIntent, PresetMarketSideEffect>(PresetMarketState()) {

    val recentPresetsPaging: Flow<PagingData<MarketPreset>> =
        getRecentPresetsPagerUseCase().cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                updateState { copy(currentUser = user) }
            }
        }
        handleIntent(PresetMarketIntent.LoadTopPresets)
    }

    override fun handleIntent(intent: PresetMarketIntent) {
        when (intent) {
            is PresetMarketIntent.LoadTopPresets -> loadTopPresets()
            is PresetMarketIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
            is PresetMarketIntent.ClickPreset -> sendEffect(PresetMarketSideEffect.NavigateToDetail(intent.presetId))
            is PresetMarketIntent.SignInWithGoogle -> signIn(intent.idToken)
            is PresetMarketIntent.SignOut -> signOut()
            is PresetMarketIntent.NavigateToSearch -> sendEffect(PresetMarketSideEffect.NavigateToSearch)
            is PresetMarketIntent.NavigateToUserInfo -> sendEffect(PresetMarketSideEffect.NavigateToUserInfo)
            is PresetMarketIntent.DismissError -> updateState { copy(error = null) }
        }
    }

    private fun loadTopPresets() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val downloadsResult = getTopDownloadPresetsUseCase()
            val likesResult = getTopLikePresetsUseCase()
            val error = downloadsResult.exceptionOrNull() ?: likesResult.exceptionOrNull()
            updateState {
                copy(
                    isLoading = false,
                    topDownloadPresets = downloadsResult.getOrDefault(emptyList()),
                    topLikePresets = likesResult.getOrDefault(emptyList()),
                    error = null,
                )
            }
            if (error != null) {
                if (error.isNetworkDisconnected()) {
                    sendEffect(PresetMarketSideEffect.ShowNetworkError)
                } else {
                    sendEffect(PresetMarketSideEffect.ShowError(error.getErrorMessage("인기 프리셋 불러오기")))
                }
            }
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { user ->
                    updateState { copy(currentUser = user) }
                    sendEffect(PresetMarketSideEffect.SignInSuccess)
                }
                .onFailure { error ->
                    if (error.isNetworkDisconnected()) {
                        sendEffect(PresetMarketSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetMarketSideEffect.ShowError(error.getErrorMessage("로그인")))
                    }
                }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            updateState { copy(currentUser = null) }
        }
    }
}
