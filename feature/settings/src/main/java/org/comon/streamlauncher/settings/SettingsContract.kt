package org.comon.streamlauncher.settings

import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
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
    val uploadProgress: PresetOperationProgress? = null,
    val pendingUploadPresetName: String? = null,
    val isSignedIn: Boolean = false,
    val liveWallpapers: List<LiveWallpaper> = emptyList(),
    val selectedOrientationTab: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    val selectedLiveWallpaperUri: String? = null,
    val selectedLiveWallpaperId: Int? = null,
    val selectedLiveWallpaperLandscapeUri: String? = null,
    val selectedLiveWallpaperLandscapeId: Int? = null,
    /** DataStore에 실제로 저장된(배경화면으로 설정된) 세로 배경화면 ID. UI 선택과 무관하게 DataStore 값만 반영 */
    val activePortraitWallpaperId: Int? = null,
    /** DataStore에 실제로 저장된(배경화면으로 설정된) 가로 배경화면 ID. UI 선택과 무관하게 DataStore 값만 반영 */
    val activeLandscapeWallpaperId: Int? = null,
    /** WallpaperManager 기준 우리 앱의 라이브 배경화면 서비스가 실제로 활성화돼 있는지 여부 */
    val isLiveWallpaperServiceActive: Boolean = false,
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
        val isLiveWallpaper: Boolean = false,
        val wallpaperLandscapeUri: String? = null,
        val isLiveWallpaperLandscape: Boolean = false,
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
    data object PauseUpload : SettingsIntent
    data object ResumeUpload : SettingsIntent
    data object CancelUpload : SettingsIntent

    data class LoadLiveWallpaperFile(val uri: String) : SettingsIntent
    data class CreateLiveWallpaper(val name: String) : SettingsIntent
    data class SelectLiveWallpaper(val id: Int, val uri: String) : SettingsIntent
    data class SetActiveLiveWallpaper(
        val id: Int,
        val uri: String,
        val orientation: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    ) : SettingsIntent
    data class DeleteLiveWallpaper(val id: Int) : SettingsIntent
    data class ClearActiveLiveWallpaper(
        val orientation: WallpaperOrientation = WallpaperOrientation.PORTRAIT,
    ) : SettingsIntent
    data class SwitchOrientationTab(val orientation: WallpaperOrientation) : SettingsIntent
    /** 화면 재진입(ON_RESUME) 시 WallpaperManager로 실제 서비스 활성 여부를 재확인 */
    data object CheckActiveWallpaper : SettingsIntent
}

sealed interface SettingsSideEffect : UiSideEffect {
    data object NavigateToMain : SettingsSideEffect
    data class StartUploadService(val presetName: String) : SettingsSideEffect
    data class UploadStarted(val presetName: String) : SettingsSideEffect
    data object UploadSuccess : SettingsSideEffect
    data class UploadError(val message: String) : SettingsSideEffect
    data object RequireSignIn : SettingsSideEffect
    data object StopUploadService : SettingsSideEffect
    data class ShowError(val message: String) : SettingsSideEffect
    data object LaunchLiveWallpaperPicker : SettingsSideEffect
    data object ReloadWallpaper : SettingsSideEffect
    data object ShowNetworkError : SettingsSideEffect
}
