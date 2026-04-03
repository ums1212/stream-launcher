package org.comon.streamlauncher.preset_market

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.GetCurrentMarketUserUseCase
import org.comon.streamlauncher.domain.usecase.ReportMarketPresetUseCase
import org.comon.streamlauncher.domain.usecase.UploadReportImageUseCase
import org.comon.streamlauncher.preset_market.navigation.MarketReport
import org.comon.streamlauncher.ui.BaseViewModel
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import javax.inject.Inject

@HiltViewModel
class ReportPresetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectivityChecker: NetworkConnectivityChecker,
    private val getCurrentMarketUserUseCase: GetCurrentMarketUserUseCase,
    private val reportMarketPresetUseCase: ReportMarketPresetUseCase,
    private val uploadReportImageUseCase: UploadReportImageUseCase,
) : BaseViewModel<ReportPresetState, ReportPresetIntent, ReportPresetSideEffect>(ReportPresetState()) {

    private val route = savedStateHandle.toRoute<MarketReport>()
    private val presetId: String = route.presetId
    private val presetName: String = route.presetName
    private val presetAuthorUid: String = route.presetAuthorUid
    private val presetAuthorDisplayName: String = route.presetAuthorDisplayName

    init {
        val reporter = getCurrentMarketUserUseCase()
        val authorName = presetAuthorDisplayName
        val name = presetName
        updateState {
            copy(
                reporterDisplayName = reporter?.displayName ?: "",
                presetAuthorDisplayName = authorName,
                presetName = name,
            )
        }
    }

    override fun handleIntent(intent: ReportPresetIntent) {
        when (intent) {
            is ReportPresetIntent.UpdateReason -> updateState { copy(reason = intent.reason) }
            is ReportPresetIntent.SelectImage -> updateState { copy(selectedImageUri = intent.uri) }
            is ReportPresetIntent.RemoveImage -> updateState { copy(selectedImageUri = null) }
            is ReportPresetIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val reason = currentState.reason.trim()
        if (reason.isEmpty()) {
            sendEffect(ReportPresetSideEffect.ShowError("신고 사유를 입력해주세요"))
            return
        }
        val reporter = getCurrentMarketUserUseCase() ?: return
        val imageUri = currentState.selectedImageUri
        updateState { copy(isSubmitting = true) }
        viewModelScope.launch {
            val imageUrl: String? = if (imageUri != null) {
                uploadReportImageUseCase(imageUri, reporter.uid)
                    .onFailure { error ->
                        updateState { copy(isSubmitting = false) }
                        if (connectivityChecker.isUnavailable()) {
                            sendEffect(ReportPresetSideEffect.ShowNetworkError)
                        } else {
                            sendEffect(ReportPresetSideEffect.ShowError(error.getErrorMessage("이미지 업로드")))
                        }
                        return@launch
                    }
                    .getOrNull()
            } else null

            reportMarketPresetUseCase(
                reporterUid = reporter.uid,
                reporterDisplayName = reporter.displayName,
                presetId = presetId,
                presetName = presetName,
                presetAuthorUid = presetAuthorUid,
                presetAuthorDisplayName = presetAuthorDisplayName,
                reason = reason,
                imageUrl = imageUrl,
            ).onSuccess {
                sendEffect(ReportPresetSideEffect.ReportSuccess)
            }.onFailure { error ->
                updateState { copy(isSubmitting = false) }
                if (connectivityChecker.isUnavailable()) {
                    sendEffect(ReportPresetSideEffect.ShowNetworkError)
                } else {
                    sendEffect(ReportPresetSideEffect.ShowError(error.getErrorMessage("신고 처리")))
                }
            }
        }
    }
}
