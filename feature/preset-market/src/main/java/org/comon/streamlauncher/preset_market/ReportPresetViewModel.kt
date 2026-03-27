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
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class ReportPresetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
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
                    .onFailure {
                        updateState { copy(isSubmitting = false) }
                        val isNetworkError = it is UnknownHostException ||
                            it.javaClass.name == "com.google.firebase.FirebaseNetworkException"
                        if (isNetworkError) {
                            sendEffect(ReportPresetSideEffect.NetworkError)
                        } else {
                            sendEffect(ReportPresetSideEffect.ShowError("이미지 업로드에 실패하였습니다. 잠시 후 다시 시도해주세요."))
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
            }.onFailure { e ->
                updateState { copy(isSubmitting = false) }
                val isNetworkError = e is UnknownHostException ||
                    e.javaClass.name == "com.google.firebase.FirebaseNetworkException"
                if (isNetworkError) {
                    sendEffect(ReportPresetSideEffect.NetworkError)
                } else {
                    sendEffect(ReportPresetSideEffect.ShowError("신고 처리가 실패하였습니다. 잠시 후 다시 시도해주세요."))
                }
            }
        }
    }
}
