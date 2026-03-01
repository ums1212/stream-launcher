package org.comon.streamlauncher.settings

import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState



data class SettingsState(
    val colorPresetIndex: Int = 0,
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
    val cellAssignments: Map<GridCell, List<String>> = emptyMap(),
    val chzzkChannelId: String = "",
    val youtubeChannelId: String = "",
    val appDrawerGridColumns: Int = 4,
    val appDrawerGridRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,
    val showNoticeDialog: Boolean = false,
    val presets: List<Preset> = emptyList(),
    val isUploading: Boolean = false,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class ChangeAccentColor(val presetIndex: Int) : SettingsIntent
    data class SetGridImage(val cell: GridCell, val type: ImageType, val uri: String) : SettingsIntent
    data class SaveFeedSettings(
        val chzzkChannelId: String,
        val youtubeChannelId: String,
    ) : SettingsIntent
    data class SaveAppDrawerSettings(val columns: Int, val rows: Int, val iconSizeRatio: Float) : SettingsIntent
    data object ShowNotice : SettingsIntent
    data object DismissNotice : SettingsIntent
    data object ResetTab : SettingsIntent
    data class SavePreset(
        val name: String,
        val saveHome: Boolean,
        val saveFeed: Boolean,
        val saveDrawer: Boolean,
        val saveWallpaper: Boolean,
        val saveTheme: Boolean,
        val wallpaperUri: String? = null,
    ) : SettingsIntent
    data class LoadPreset(
        val preset: Preset,
        val loadHome: Boolean,
        val loadFeed: Boolean,
        val loadDrawer: Boolean,
        val loadWallpaper: Boolean,
        val loadTheme: Boolean
    ) : SettingsIntent
    data class DeletePreset(val preset: Preset) : SettingsIntent
    data object ResetAllGridImages : SettingsIntent
    data class SignInWithGoogle(val idToken: String) : SettingsIntent
    data class UploadPreset(
        val preset: Preset,
        val description: String,
        val tags: List<String>,
        val previewUris: List<String>,
    ) : SettingsIntent
}

sealed interface SettingsSideEffect : UiSideEffect {
    data object NavigateToMain : SettingsSideEffect
    data object UploadSuccess : SettingsSideEffect
    data class UploadError(val message: String) : SettingsSideEffect
    data object RequireSignIn : SettingsSideEffect
}
