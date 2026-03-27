package org.comon.streamlauncher.preset_market

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.GetCurrentMarketUserUseCase
import org.comon.streamlauncher.domain.usecase.GetUserPresetsUseCase  // core:domain에 위치
import org.comon.streamlauncher.domain.usecase.SignOutUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.network.error.isNetworkDisconnected
import javax.inject.Inject

@HiltViewModel
class PresetMarketUserInfoViewModel @Inject constructor(
    getCurrentMarketUserUseCase: GetCurrentMarketUserUseCase,
    private val getUserPresetsUseCase: GetUserPresetsUseCase,
    private val signOutUseCase: SignOutUseCase,
) : BaseViewModel<PresetMarketUserInfoState, PresetMarketUserInfoIntent, PresetMarketUserInfoSideEffect>(
    PresetMarketUserInfoState()
) {

    init {
        val user = getCurrentMarketUserUseCase()
        updateState { copy(user = user) }
        if (user != null) {
            handleIntent(PresetMarketUserInfoIntent.LoadUserPresets)
        }
    }

    override fun handleIntent(intent: PresetMarketUserInfoIntent) {
        when (intent) {
            is PresetMarketUserInfoIntent.LoadUserPresets -> loadUserPresets()
            is PresetMarketUserInfoIntent.SignOut -> signOut()
            is PresetMarketUserInfoIntent.ClickPreset -> sendEffect(
                PresetMarketUserInfoSideEffect.NavigateToDetail(intent.presetId)
            )
        }
    }

    private fun loadUserPresets() {
        val uid = currentState.user?.uid ?: return
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            getUserPresetsUseCase(uid)
                .onSuccess { presets ->
                    updateState { copy(isLoading = false, presets = presets) }
                }
                .onFailure { e ->
                    updateState { copy(isLoading = false) }
                    if (e.isNetworkDisconnected()) {
                        sendEffect(PresetMarketUserInfoSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetMarketUserInfoSideEffect.ShowError(e.getErrorMessage("프리셋 목록 불러오기")))
                    }
                }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            updateState { copy(user = null, presets = emptyList()) }
            sendEffect(PresetMarketUserInfoSideEffect.SignedOut)
        }
    }
}
