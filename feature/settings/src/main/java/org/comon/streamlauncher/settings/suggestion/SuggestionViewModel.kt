package org.comon.streamlauncher.settings.suggestion

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.SubmitSuggestionUseCase
import org.comon.streamlauncher.domain.usecase.UploadSuggestionImageUseCase
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SuggestionViewModel @Inject constructor(
    private val connectivityChecker: NetworkConnectivityChecker,
    private val submitSuggestionUseCase: SubmitSuggestionUseCase,
    private val uploadSuggestionImageUseCase: UploadSuggestionImageUseCase,
) : BaseViewModel<SuggestionState, SuggestionIntent, SuggestionSideEffect>(SuggestionState()) {

    override fun handleIntent(intent: SuggestionIntent) {
        when (intent) {
            is SuggestionIntent.UpdateEmail -> updateState { copy(email = intent.email) }
            is SuggestionIntent.UpdateBody -> updateState { copy(body = intent.body) }
            is SuggestionIntent.SelectImage -> updateState { copy(selectedImageUri = intent.uri) }
            is SuggestionIntent.RemoveImage -> updateState { copy(selectedImageUri = null) }
            is SuggestionIntent.ShowImageFormatError -> sendEffect(SuggestionSideEffect.ShowError(intent.message))
            is SuggestionIntent.Submit -> submit(intent.appVersion, intent.deviceInfo)
        }
    }

    private fun submit(appVersion: String, deviceInfo: String) {
        val body = currentState.body.trim()
        if (body.isEmpty()) {
            sendEffect(SuggestionSideEffect.ShowError("건의 내용을 입력해주세요"))
            return
        }
        val imageUri = currentState.selectedImageUri
        updateState { copy(isSubmitting = true) }
        viewModelScope.launch {
            val imageUrl: String? = if (imageUri != null) {
                uploadSuggestionImageUseCase(imageUri)
                    .onFailure { error ->
                        updateState { copy(isSubmitting = false) }
                        if (connectivityChecker.isUnavailable()) {
                            sendEffect(SuggestionSideEffect.ShowNetworkError)
                        } else {
                            sendEffect(SuggestionSideEffect.ShowError(error.getErrorMessage("이미지 업로드")))
                        }
                        return@launch
                    }
                    .getOrNull()
            } else null

            submitSuggestionUseCase(
                email = currentState.email.trim(),
                body = body,
                imageUrl = imageUrl,
                appVersion = appVersion,
                deviceInfo = deviceInfo,
            ).onSuccess {
                sendEffect(SuggestionSideEffect.SubmitSuccess)
            }.onFailure { error ->
                updateState { copy(isSubmitting = false) }
                if (connectivityChecker.isUnavailable()) {
                    sendEffect(SuggestionSideEffect.ShowNetworkError)
                } else {
                    sendEffect(SuggestionSideEffect.ShowError(error.getErrorMessage("건의 접수")))
                }
            }
        }
    }
}
