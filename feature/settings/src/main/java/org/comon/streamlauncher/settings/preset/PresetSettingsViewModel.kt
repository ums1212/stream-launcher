package org.comon.streamlauncher.settings.preset

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.usecase.DeletePresetUseCase
import org.comon.streamlauncher.domain.usecase.GetAllLiveWallpapersUseCase
import org.comon.streamlauncher.domain.usecase.GetAllPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.ObserveAuthStateUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.usecase.SavePresetUseCase
import org.comon.streamlauncher.domain.usecase.SetLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SetStaticWallpaperUseCase
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.usecase.UpdateMarketPresetIdUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.settings.upload.UploadDataHolder
import org.comon.streamlauncher.settings.upload.UploadProgressTracker
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PresetSettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val getAllPresetsUseCase: GetAllPresetsUseCase,
    private val savePresetUseCase: SavePresetUseCase,
    private val deletePresetUseCase: DeletePresetUseCase,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
    private val saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase,
    private val setStaticWallpaperUseCase: SetStaticWallpaperUseCase,
    private val wallpaperHelper: WallpaperHelper,
    private val getAllLiveWallpapersUseCase: GetAllLiveWallpapersUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val uploadProgressTracker: UploadProgressTracker,
    private val uploadDataHolder: UploadDataHolder,
    private val updateMarketPresetIdUseCase: UpdateMarketPresetIdUseCase,
    private val connectivityChecker: NetworkConnectivityChecker,
) : BaseViewModel<PresetSettingsState, PresetSettingsIntent, PresetSettingsSideEffect>(PresetSettingsState()) {

    private var cachedSettings: LauncherSettings = LauncherSettings()
    private var cachedMarketUser: MarketUser? = null
    private var pendingUploadIntent: PresetSettingsIntent.UploadPreset? = null
    private var pendingUploadLocalPresetId: Int = 0

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                cachedSettings = settings
            }
        }
        viewModelScope.launch {
            getAllPresetsUseCase().collect { presets ->
                updateState { copy(presets = presets) }
            }
        }
        viewModelScope.launch {
            getAllLiveWallpapersUseCase().collect { list ->
                updateState { copy(liveWallpapers = list) }
            }
        }
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                cachedMarketUser = user
                updateState { copy(isSignedIn = user != null) }
                if (user != null) {
                    pendingUploadIntent?.let { handleIntent(it) }
                }
            }
        }
        viewModelScope.launch {
            uploadProgressTracker.progress.collect { progress ->
                updateState { copy(uploadProgress = progress, pendingUploadPresetName = null) }
                if (progress?.isCompleted == true) {
                    val localId = pendingUploadLocalPresetId
                    val marketId = progress.marketPresetId
                    if (localId != 0 && marketId != null) {
                        updateMarketPresetIdUseCase(localId, marketId)
                    }
                    sendEffect(PresetSettingsSideEffect.UploadSuccess)
                } else {
                    progress?.error?.let { error ->
                        if (connectivityChecker.isUnavailable()) {
                            sendEffect(PresetSettingsSideEffect.ShowNetworkError)
                        } else {
                            sendEffect(PresetSettingsSideEffect.UploadError(error.getErrorMessage("업로드")))
                        }
                    }
                }
            }
        }
    }

    override fun handleIntent(intent: PresetSettingsIntent) {
        when (intent) {
            is PresetSettingsIntent.SavePreset -> savePreset(intent)
            is PresetSettingsIntent.LoadPreset -> loadPreset(intent)
            is PresetSettingsIntent.DeletePreset -> deletePreset(intent.preset)
            is PresetSettingsIntent.UploadPreset -> uploadPreset(intent)
            is PresetSettingsIntent.PauseUpload -> uploadProgressTracker.pause()
            is PresetSettingsIntent.ResumeUpload -> uploadProgressTracker.resume()
            is PresetSettingsIntent.CancelUpload -> cancelUpload()
        }
    }

    private fun savePreset(intent: PresetSettingsIntent.SavePreset) {
        viewModelScope.launch {
            val settings = cachedSettings
            val wallpaperPath = if (intent.saveWallpaper && intent.wallpaperUri != null && !intent.isLiveWallpaper) {
                wallpaperHelper.copyWallpaperFromUri(intent.wallpaperUri, System.currentTimeMillis())
            } else null

            val staticWallpaperLandscapePath = if (intent.saveWallpaper && intent.staticWallpaperLandscapeUri != null && !intent.isLiveWallpaper) {
                wallpaperHelper.copyWallpaperFromUri(intent.staticWallpaperLandscapeUri, System.currentTimeMillis() + 1L)
            } else null

            val preset = Preset(
                name = intent.name,
                hasTopLeftImage = intent.saveHome && settings.gridCellImages[GridCell.TOP_LEFT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasTopRightImage = intent.saveHome && settings.gridCellImages[GridCell.TOP_RIGHT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasBottomLeftImage = intent.saveHome && settings.gridCellImages[GridCell.BOTTOM_LEFT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasBottomRightImage = intent.saveHome && settings.gridCellImages[GridCell.BOTTOM_RIGHT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                topLeftIdleUri = if (intent.saveHome) settings.gridCellImages[GridCell.TOP_LEFT]?.idleImageUri else null,
                topLeftExpandedUri = if (intent.saveHome) settings.gridCellImages[GridCell.TOP_LEFT]?.expandedImageUri else null,
                topRightIdleUri = if (intent.saveHome) settings.gridCellImages[GridCell.TOP_RIGHT]?.idleImageUri else null,
                topRightExpandedUri = if (intent.saveHome) settings.gridCellImages[GridCell.TOP_RIGHT]?.expandedImageUri else null,
                bottomLeftIdleUri = if (intent.saveHome) settings.gridCellImages[GridCell.BOTTOM_LEFT]?.idleImageUri else null,
                bottomLeftExpandedUri = if (intent.saveHome) settings.gridCellImages[GridCell.BOTTOM_LEFT]?.expandedImageUri else null,
                bottomRightIdleUri = if (intent.saveHome) settings.gridCellImages[GridCell.BOTTOM_RIGHT]?.idleImageUri else null,
                bottomRightExpandedUri = if (intent.saveHome) settings.gridCellImages[GridCell.BOTTOM_RIGHT]?.expandedImageUri else null,
                hasFeedSettings = intent.saveFeed,
                youtubeChannelId = if (intent.saveFeed) settings.youtubeChannelId else "",
                chzzkChannelId = if (intent.saveFeed) settings.chzzkChannelId else "",
                hasAppDrawerSettings = intent.saveDrawer,
                appDrawerColumns = if (intent.saveDrawer) settings.appDrawerGridColumns else 4,
                appDrawerRows = if (intent.saveDrawer) settings.appDrawerGridRows else 6,
                appDrawerIconSizeRatio = if (intent.saveDrawer) settings.appDrawerIconSizeRatio else 1.0f,
                hasWallpaperSettings = intent.saveWallpaper,
                wallpaperUri = if (intent.isLiveWallpaper) null else wallpaperPath,
                staticWallpaperLandscapeUri = if (intent.isLiveWallpaper) null else staticWallpaperLandscapePath,
                hasThemeSettings = intent.saveTheme,
                themeColorHex = if (intent.saveTheme) settings.colorPresetIndex.toString() else null,
                isLiveWallpaper = intent.isLiveWallpaper,
                liveWallpaperUri = if (intent.isLiveWallpaper) intent.wallpaperUri else null,
                isLiveWallpaperLandscape = intent.isLiveWallpaperLandscape,
                liveWallpaperLandscapeUri = if (intent.isLiveWallpaperLandscape) intent.wallpaperLandscapeUri else null,
            )
            runCatching { savePresetUseCase(preset) }
                .onFailure { error ->
                    if (connectivityChecker.isUnavailable()) sendEffect(PresetSettingsSideEffect.ShowNetworkError)
                    else sendEffect(PresetSettingsSideEffect.ShowError(error.getErrorMessage("프리셋 저장")))
                }
        }
    }

    private fun loadPreset(intent: PresetSettingsIntent.LoadPreset) {
        viewModelScope.launch {
            runCatching {
                val preset = intent.preset
                if (intent.loadHome) {
                    if (preset.hasTopLeftImage) saveGridCellImageUseCase(GridCell.TOP_LEFT, preset.topLeftIdleUri, preset.topLeftExpandedUri)
                    if (preset.hasTopRightImage) saveGridCellImageUseCase(GridCell.TOP_RIGHT, preset.topRightIdleUri, preset.topRightExpandedUri)
                    if (preset.hasBottomLeftImage) saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, preset.bottomLeftIdleUri, preset.bottomLeftExpandedUri)
                    if (preset.hasBottomRightImage) saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, preset.bottomRightIdleUri, preset.bottomRightExpandedUri)
                }
                if (intent.loadFeed && preset.hasFeedSettings) {
                    saveFeedSettingsUseCase(preset.chzzkChannelId, preset.youtubeChannelId)
                }
                if (intent.loadDrawer && preset.hasAppDrawerSettings) {
                    saveAppDrawerSettingsUseCase(preset.appDrawerColumns, preset.appDrawerRows, preset.appDrawerIconSizeRatio)
                }
                if (intent.loadTheme && preset.hasThemeSettings) {
                    preset.themeColorHex?.toIntOrNull()?.let { saveColorPresetUseCase(it) }
                }
                if (intent.loadWallpaper && preset.hasWallpaperSettings) {
                    if (preset.isLiveWallpaper && preset.liveWallpaperUri != null) {
                        setLiveWallpaperUseCase(preset.id, preset.liveWallpaperUri, WallpaperOrientation.PORTRAIT)
                        sendEffect(PresetSettingsSideEffect.LaunchLiveWallpaperPicker)
                    } else {
                        preset.wallpaperUri?.let { uri ->
                            setStaticWallpaperUseCase(uri, WallpaperOrientation.PORTRAIT)?.let { filePath ->
                                if (filePath.isNotEmpty()) wallpaperHelper.setWallpaperFromPreset(filePath)
                            }
                        }
                        preset.staticWallpaperLandscapeUri?.let { uri ->
                            setStaticWallpaperUseCase(uri, WallpaperOrientation.LANDSCAPE)
                        }
                    }
                    if (preset.isLiveWallpaperLandscape && preset.liveWallpaperLandscapeUri != null) {
                        setLiveWallpaperUseCase(preset.id, preset.liveWallpaperLandscapeUri, WallpaperOrientation.LANDSCAPE)
                    }
                }
            }.onFailure { error ->
                if (connectivityChecker.isUnavailable()) sendEffect(PresetSettingsSideEffect.ShowNetworkError)
                else sendEffect(PresetSettingsSideEffect.ShowError(error.getErrorMessage("프리셋 적용")))
            }
        }
    }

    private fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            runCatching {
                deletePresetUseCase(preset)
                preset.wallpaperUri?.let { wallpaperHelper.deletePresetWallpaper(it) }
                preset.staticWallpaperLandscapeUri?.let { wallpaperHelper.deletePresetWallpaper(it) }
            }.onFailure { error ->
                if (connectivityChecker.isUnavailable()) sendEffect(PresetSettingsSideEffect.ShowNetworkError)
                else sendEffect(PresetSettingsSideEffect.ShowError(error.getErrorMessage("프리셋 삭제")))
            }
        }
    }

    private fun uploadPreset(intent: PresetSettingsIntent.UploadPreset) {
        if (cachedMarketUser == null) {
            pendingUploadIntent = intent
            sendEffect(PresetSettingsSideEffect.RequireSignIn)
            return
        }
        pendingUploadIntent = null

        val p = intent.preset
        pendingUploadLocalPresetId = p.id

        uploadDataHolder.pendingPreset = p
        uploadDataHolder.pendingPreviewUris = intent.previewUris
        uploadDataHolder.pendingDescription = intent.description
        uploadDataHolder.pendingTags = intent.tags

        updateState { copy(pendingUploadPresetName = p.name) }

        sendEffect(PresetSettingsSideEffect.StartUploadService(p.name))
        sendEffect(PresetSettingsSideEffect.UploadStarted(p.name))
    }

    private fun cancelUpload() {
        uploadProgressTracker.clear()
        updateState { copy(uploadProgress = null, pendingUploadPresetName = null) }
        pendingUploadLocalPresetId = 0
        sendEffect(PresetSettingsSideEffect.StopUploadService)
    }
}
