package org.comon.streamlauncher.preset_market

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.data.usecase.DownloadMarketPresetUseCase
import org.comon.streamlauncher.domain.usecase.GetCurrentMarketUserUseCase
import org.comon.streamlauncher.domain.usecase.GetMarketPresetDetailUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.usecase.ToggleMarketPresetLikeUseCase
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PresetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMarketPresetDetailUseCase: GetMarketPresetDetailUseCase,
    private val getCurrentMarketUserUseCase: GetCurrentMarketUserUseCase,
    private val toggleMarketPresetLikeUseCase: ToggleMarketPresetLikeUseCase,
    private val downloadMarketPresetUseCase: DownloadMarketPresetUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val marketRepository: MarketPresetRepository,
) : BaseViewModel<PresetDetailState, PresetDetailIntent, PresetDetailSideEffect>(PresetDetailState()) {

    // 로그인 후 재실행할 대기 중인 액션
    private var pendingAction: (() -> Unit)? = null

    init {
        val presetId = savedStateHandle.get<String>("presetId")
        if (presetId != null) {
            handleIntent(PresetDetailIntent.LoadPreset(presetId))
        }
    }

    override fun handleIntent(intent: PresetDetailIntent) {
        when (intent) {
            is PresetDetailIntent.LoadPreset -> loadPreset(intent.presetId)
            is PresetDetailIntent.ToggleLike -> ensureSignedIn { toggleLike() }
            is PresetDetailIntent.DownloadPreset -> ensureSignedIn { downloadPreset() }
            is PresetDetailIntent.SignInWithGoogle -> signIn(intent.idToken)
        }
    }

    private fun ensureSignedIn(action: () -> Unit) {
        if (getCurrentMarketUserUseCase() != null) {
            action()
        } else {
            pendingAction = action
            sendEffect(PresetDetailSideEffect.RequireSignIn)
        }
    }

    private fun loadPreset(presetId: String) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            getMarketPresetDetailUseCase(presetId)
                .onSuccess { preset ->
                    updateState { copy(preset = preset, isLoading = false) }
                    // 좋아요 상태 확인
                    if (getCurrentMarketUserUseCase() != null) {
                        marketRepository.isLikedByCurrentUser(presetId)
                            .onSuccess { liked -> updateState { copy(isLiked = liked) } }
                    }
                }
                .onFailure { e ->
                    updateState { copy(isLoading = false, error = e.message) }
                    sendEffect(PresetDetailSideEffect.ShowError(e.message ?: "로드 실패"))
                }
        }
    }

    private fun toggleLike() {
        val presetId = currentState.preset?.id ?: return
        viewModelScope.launch {
            toggleMarketPresetLikeUseCase(presetId)
                .onSuccess { isLikedNow ->
                    updateState {
                        copy(
                            isLiked = isLikedNow,
                            preset = preset?.copy(
                                likeCount = if (isLikedNow) {
                                    (preset.likeCount + 1)
                                } else {
                                    maxOf(0, preset.likeCount - 1)
                                }
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    sendEffect(PresetDetailSideEffect.ShowError(e.message ?: "좋아요 실패"))
                }
        }
    }

    private fun downloadPreset() {
        val preset = currentState.preset ?: return
        updateState { copy(isDownloading = true) }
        viewModelScope.launch {
            downloadMarketPresetUseCase(preset)
                .onSuccess {
                    updateState { copy(isDownloading = false) }
                    sendEffect(PresetDetailSideEffect.DownloadComplete)
                }
                .onFailure { e ->
                    updateState { copy(isDownloading = false) }
                    sendEffect(PresetDetailSideEffect.ShowError(e.message ?: "다운로드 실패"))
                }
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    // 로그인 성공 후 대기 중인 액션 재실행
                    pendingAction?.invoke()
                    pendingAction = null
                }
                .onFailure { e ->
                    pendingAction = null
                    sendEffect(PresetDetailSideEffect.ShowError(e.message ?: "로그인 실패"))
                }
        }
    }
}
