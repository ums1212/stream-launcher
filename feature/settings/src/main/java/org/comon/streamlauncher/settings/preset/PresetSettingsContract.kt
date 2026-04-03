package org.comon.streamlauncher.settings.preset

import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class PresetSettingsState(
    val presets: List<Preset> = emptyList(),
    val liveWallpapers: List<LiveWallpaper> = emptyList(),
    val isSignedIn: Boolean = false,
    val uploadProgress: PresetOperationProgress? = null,
    val pendingUploadPresetName: String? = null,
) : UiState

sealed interface PresetSettingsIntent : UiIntent {
    data class SavePreset(
        val name: String,
        val saveHome: Boolean,
        val saveFeed: Boolean,
        val saveDrawer: Boolean,
        val saveWallpaper: Boolean,
        val saveTheme: Boolean,
        val wallpaperUri: String? = null,
        val isLiveWallpaper: Boolean = false,
        val wallpaperLandscapeUri: String? = null,
        val isLiveWallpaperLandscape: Boolean = false,
        val staticWallpaperLandscapeUri: String? = null,
    ) : PresetSettingsIntent
    data class LoadPreset(
        val preset: Preset,
        val loadHome: Boolean,
        val loadFeed: Boolean,
        val loadDrawer: Boolean,
        val loadWallpaper: Boolean,
        val loadTheme: Boolean,
    ) : PresetSettingsIntent
    data class DeletePreset(val preset: Preset) : PresetSettingsIntent
    data class UploadPreset(
        val preset: Preset,
        val description: String,
        val tags: List<String>,
        val previewUris: List<String>,
    ) : PresetSettingsIntent
    data object PauseUpload : PresetSettingsIntent
    data object ResumeUpload : PresetSettingsIntent
    data object CancelUpload : PresetSettingsIntent
}

sealed interface PresetSettingsSideEffect : UiSideEffect {
    data object RequireSignIn : PresetSettingsSideEffect
    data class StartUploadService(val presetName: String) : PresetSettingsSideEffect
    data class UploadStarted(val presetName: String) : PresetSettingsSideEffect
    data object UploadSuccess : PresetSettingsSideEffect
    data class UploadError(val message: String) : PresetSettingsSideEffect
    data object StopUploadService : PresetSettingsSideEffect
    data object LaunchLiveWallpaperPicker : PresetSettingsSideEffect
    data class ShowError(val message: String) : PresetSettingsSideEffect
    data object ShowNetworkError : PresetSettingsSideEffect
}
