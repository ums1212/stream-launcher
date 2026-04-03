package org.comon.streamlauncher.preset_market

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.DeleteMarketPresetUseCase
import org.comon.streamlauncher.domain.usecase.GetAllPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetCurrentMarketUserUseCase
import org.comon.streamlauncher.domain.usecase.GetMarketPresetDetailUseCase
import org.comon.streamlauncher.domain.usecase.IsDownloadedByMarketIdUseCase
import org.comon.streamlauncher.domain.usecase.IsLikedByCurrentUserUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.usecase.ToggleMarketPresetLikeUseCase
import org.comon.streamlauncher.preset_market.download.DownloadDataHolder
import org.comon.streamlauncher.preset_market.download.DownloadProgressTracker
import org.comon.streamlauncher.ui.BaseViewModel
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import javax.inject.Inject

@HiltViewModel
class PresetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectivityChecker: NetworkConnectivityChecker,
    private val getMarketPresetDetailUseCase: GetMarketPresetDetailUseCase,
    private val getCurrentMarketUserUseCase: GetCurrentMarketUserUseCase,
    private val toggleMarketPresetLikeUseCase: ToggleMarketPresetLikeUseCase,
    private val getAllPresetsUseCase: GetAllPresetsUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val isLikedByCurrentUserUseCase: IsLikedByCurrentUserUseCase,
    private val isDownloadedByMarketIdUseCase: IsDownloadedByMarketIdUseCase,
    private val downloadDataHolder: DownloadDataHolder,
    private val downloadProgressTracker: DownloadProgressTracker,
    private val deleteMarketPresetUseCase: DeleteMarketPresetUseCase,
) : BaseViewModel<PresetDetailState, PresetDetailIntent, PresetDetailSideEffect>(PresetDetailState()) {

    // 로그인 후 재실행할 대기 중인 액션
    private var pendingAction: (() -> Unit)? = null

    init {
        val presetId = savedStateHandle.get<String>("presetId")
        if (presetId != null) {
            handleIntent(PresetDetailIntent.LoadPreset(presetId))
        }

        viewModelScope.launch {
            downloadProgressTracker.progress.collect { progress ->
                updateState { copy(downloadProgress = progress, isDownloading = progress != null) }
                when {
                    progress?.isCompleted == true -> {
                        updateState { copy(isAlreadyDownloaded = true) }
                        sendEffect(PresetDetailSideEffect.DownloadComplete)
                    }
                    progress?.error != null -> {
                        val error = progress.error!!
                        if (connectivityChecker.isUnavailable()) {
                            sendEffect(PresetDetailSideEffect.ShowNetworkError)
                        } else {
                            sendEffect(PresetDetailSideEffect.ShowError(error.getErrorMessage("다운로드")))
                        }
                    }
                }
            }
        }
    }

    override fun handleIntent(intent: PresetDetailIntent) {
        when (intent) {
            is PresetDetailIntent.LoadPreset -> loadPreset(intent.presetId)
            is PresetDetailIntent.ToggleLike -> ensureSignedIn { toggleLike() }
            is PresetDetailIntent.DownloadPreset -> ensureSignedIn { downloadPreset() }
            is PresetDetailIntent.PauseDownload -> downloadProgressTracker.pause()
            is PresetDetailIntent.ResumeDownload -> downloadProgressTracker.resume()
            is PresetDetailIntent.CancelDownload -> cancelDownload()
            is PresetDetailIntent.SignInWithGoogle -> signIn(intent.idToken)
            is PresetDetailIntent.DeletePreset -> deletePreset()
            is PresetDetailIntent.ReportPreset -> ensureSignedIn { sendEffect(PresetDetailSideEffect.NavigateToReport) }
        }
    }

    private fun cancelDownload() {
        downloadProgressTracker.requestCancellation()
        updateState { copy(isDownloading = false, downloadProgress = null) }
        sendEffect(PresetDetailSideEffect.StopDownloadService)
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
                    val currentUserUid = getCurrentMarketUserUseCase()?.uid
                    updateState {
                        copy(
                            preset = preset,
                            isLoading = false,
                            isOwnPreset = currentUserUid != null && currentUserUid == preset.authorUid,
                        )
                    }
                    // 다운로드 여부 확인
                    val isDownloaded = isDownloadedByMarketIdUseCase(presetId)
                    updateState { copy(isAlreadyDownloaded = isDownloaded) }
                    // 좋아요 상태 확인
                    if (currentUserUid != null) {
                        isLikedByCurrentUserUseCase(presetId)
                            .onSuccess { liked -> updateState { copy(isLiked = liked) } }
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false) }
                    if (connectivityChecker.isUnavailable()) {
                        sendEffect(PresetDetailSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetDetailSideEffect.ShowError(error.getErrorMessage("프리셋 정보 불러오기")))
                    }
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
                .onFailure { error ->
                    if (connectivityChecker.isUnavailable()) {
                        sendEffect(PresetDetailSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetDetailSideEffect.ShowError(error.getErrorMessage("좋아요 처리")))
                    }
                }
        }
    }

    private fun downloadPreset() {
        // 이미 다운로드 진행 중이면 무시 (tracker 또는 임시 플래그 모두 확인)
        if (downloadProgressTracker.progress.value != null || currentState.isDownloading) return

        val preset = currentState.preset ?: return
        // 서비스 기동 전에 즉시 비활성화 — 연타 방지
        updateState { copy(isDownloading = true) }
        viewModelScope.launch {
            val presetCount = getAllPresetsUseCase().first().size
            if (presetCount >= 10) {
                updateState { copy(isDownloading = false) }
                sendEffect(PresetDetailSideEffect.PresetLimitExceeded)
                return@launch
            }
            downloadDataHolder.pendingPreset = preset
            sendEffect(PresetDetailSideEffect.StartDownloadService(preset.name))
        }
    }

    private fun deletePreset() {
        val presetId = currentState.preset?.id ?: return
        viewModelScope.launch {
            deleteMarketPresetUseCase(presetId)
                .onSuccess {
                    sendEffect(PresetDetailSideEffect.DeleteComplete)
                }
                .onFailure { error ->
                    if (connectivityChecker.isUnavailable()) {
                        sendEffect(PresetDetailSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetDetailSideEffect.ShowError(error.getErrorMessage("삭제")))
                    }
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
                .onFailure { error ->
                    pendingAction = null
                    if (connectivityChecker.isUnavailable()) {
                        sendEffect(PresetDetailSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(PresetDetailSideEffect.ShowError(error.getErrorMessage("로그인")))
                    }
                }
        }
    }
}
