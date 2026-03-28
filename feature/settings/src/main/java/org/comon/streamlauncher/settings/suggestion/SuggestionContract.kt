package org.comon.streamlauncher.settings.suggestion

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class SuggestionState(
    val email: String = "",
    val body: String = "",
    val selectedImageUri: String? = null,
    val isSubmitting: Boolean = false,
) : UiState

sealed interface SuggestionIntent : UiIntent {
    data class UpdateEmail(val email: String) : SuggestionIntent
    data class UpdateBody(val body: String) : SuggestionIntent
    data class SelectImage(val uri: String) : SuggestionIntent
    data object RemoveImage : SuggestionIntent
    data class ShowImageFormatError(val message: String) : SuggestionIntent
    data class Submit(val appVersion: String, val deviceInfo: String) : SuggestionIntent
}

sealed interface SuggestionSideEffect : UiSideEffect {
    data object SubmitSuccess : SuggestionSideEffect
    data class ShowError(val message: String) : SuggestionSideEffect
    data object ShowNetworkError : SuggestionSideEffect
}
