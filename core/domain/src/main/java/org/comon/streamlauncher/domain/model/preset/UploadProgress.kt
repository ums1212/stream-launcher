package org.comon.streamlauncher.domain.model.preset

data class UploadProgress(
    val presetName: String,
    val currentStep: Int,
    val totalSteps: Int,
    val isCompleted: Boolean = false,
    val error: String? = null,
) {
    val percentage: Float get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
}
