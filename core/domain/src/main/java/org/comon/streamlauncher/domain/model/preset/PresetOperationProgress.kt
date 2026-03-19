package org.comon.streamlauncher.domain.model.preset

data class PresetOperationProgress(
    val presetName: String,
    val currentStep: Int,
    val totalSteps: Int,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val marketPresetId: String? = null,  // upload 전용 (download 시 null)
) {
    val percentage: Float get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
}
