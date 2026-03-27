package org.comon.streamlauncher.domain.model.preset

data class PresetOperationProgress(
    val presetName: String,
    val currentStep: Int,
    val totalSteps: Int,
    val isCompleted: Boolean = false,
    val error: Throwable? = null,
    val marketPresetId: String? = null,  // upload 전용 (download 시 null)
    val liveWallpaperUri: String? = null, // 라이브 배경화면 포함 다운로드 시 설정
) {
    val percentage: Float get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
}
