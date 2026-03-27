package org.comon.streamlauncher.preset_market

import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class ReportPresetState(
    val reporterDisplayName: String = "",
    val presetAuthorDisplayName: String = "",
    val presetName: String = "",
    val reason: String = "",
    val selectedImageUri: String? = null,
    val isSubmitting: Boolean = false,
) : UiState

sealed interface ReportPresetIntent : UiIntent {
    data class UpdateReason(val reason: String) : ReportPresetIntent
    data class SelectImage(val uri: String) : ReportPresetIntent
    data object RemoveImage : ReportPresetIntent
    data object Submit : ReportPresetIntent
}

sealed interface ReportPresetSideEffect : UiSideEffect {
    data object ReportSuccess : ReportPresetSideEffect
    data class ShowError(val message: String) : ReportPresetSideEffect
    data object NetworkError : ReportPresetSideEffect
}
