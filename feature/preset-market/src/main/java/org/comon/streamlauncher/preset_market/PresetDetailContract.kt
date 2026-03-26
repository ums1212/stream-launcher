package org.comon.streamlauncher.preset_market

import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class PresetDetailState(
    val preset: MarketPreset? = null,
    val isLiked: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val downloadProgress: PresetOperationProgress? = null,
    val isAlreadyDownloaded: Boolean = false,
    val isOwnPreset: Boolean = false,
) : UiState

sealed interface PresetDetailIntent : UiIntent {
    data class LoadPreset(val presetId: String) : PresetDetailIntent
    data object ToggleLike : PresetDetailIntent
    data object DownloadPreset : PresetDetailIntent
    data object PauseDownload : PresetDetailIntent
    data object ResumeDownload : PresetDetailIntent
    data object CancelDownload : PresetDetailIntent
    data class SignInWithGoogle(val idToken: String) : PresetDetailIntent
    data object DeletePreset : PresetDetailIntent
}

sealed interface PresetDetailSideEffect : UiSideEffect {
    data object DownloadComplete : PresetDetailSideEffect
    data class ShowError(val message: String) : PresetDetailSideEffect
    data object RequireSignIn : PresetDetailSideEffect
    data object PresetLimitExceeded : PresetDetailSideEffect
    data class StartDownloadService(val presetName: String) : PresetDetailSideEffect
    data object StopDownloadService : PresetDetailSideEffect
    data object DeleteComplete : PresetDetailSideEffect
}
