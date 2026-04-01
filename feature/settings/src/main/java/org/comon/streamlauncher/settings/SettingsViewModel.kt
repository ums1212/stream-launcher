package org.comon.streamlauncher.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DeleteLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.DeletePresetUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetAllLiveWallpapersUseCase
import org.comon.streamlauncher.domain.usecase.GetAllPresetsUseCase
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.usecase.ObserveAuthStateUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.usecase.SaveLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SavePresetUseCase
import org.comon.streamlauncher.domain.usecase.SetLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SetStaticWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.UpdateMarketPresetIdUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.settings.upload.UploadDataHolder
import org.comon.streamlauncher.settings.upload.UploadProgressTracker
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.ui.BaseViewModel
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.network.error.isNetworkDisconnected
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
    private val saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase,
    private val checkNoticeUseCase: CheckNoticeUseCase,
    private val dismissNoticeUseCase: DismissNoticeUseCase,
    private val getAllPresetsUseCase: GetAllPresetsUseCase,
    private val savePresetUseCase: SavePresetUseCase,
    private val deletePresetUseCase: DeletePresetUseCase,
    private val wallpaperHelper: org.comon.streamlauncher.domain.util.WallpaperHelper,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val uploadProgressTracker: UploadProgressTracker,
    private val uploadDataHolder: UploadDataHolder,
    private val updateMarketPresetIdUseCase: UpdateMarketPresetIdUseCase,
    private val getAllLiveWallpapersUseCase: GetAllLiveWallpapersUseCase,
    private val saveLiveWallpaperUseCase: SaveLiveWallpaperUseCase,
    private val deleteLiveWallpaperUseCase: DeleteLiveWallpaperUseCase,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase,
    private val setStaticWallpaperUseCase: SetStaticWallpaperUseCase,
) : BaseViewModel<SettingsState, SettingsIntent, SettingsSideEffect>(SettingsState()) {

    private var currentNoticeVersion: String = ""
    private var pendingUploadIntent: SettingsIntent.UploadPreset? = null
    private var cachedMarketUser: MarketUser? = null
    private var pendingUploadLocalPresetId: Int = 0

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        colorPresetIndex = settings.colorPresetIndex,
                        gridCellImages = settings.gridCellImages,
                        cellAssignments = settings.cellAssignments,
                        chzzkChannelId = settings.chzzkChannelId,
                        youtubeChannelId = settings.youtubeChannelId,
                        appDrawerGridColumns = settings.appDrawerGridColumns,
                        appDrawerGridRows = settings.appDrawerGridRows,
                        appDrawerIconSizeRatio = settings.appDrawerIconSizeRatio,
                        selectedLiveWallpaperId = settings.liveWallpaperId,
                        selectedLiveWallpaperUri = settings.liveWallpaperUri,
                        selectedLiveWallpaperLandscapeId = settings.liveWallpaperLandscapeId,
                        selectedLiveWallpaperLandscapeUri = settings.liveWallpaperLandscapeUri,
                        activePortraitWallpaperId = settings.liveWallpaperId,
                        activeLandscapeWallpaperId = settings.liveWallpaperLandscapeId,
                        staticWallpaperPortraitUri = settings.staticWallpaperPortraitUri,
                        staticWallpaperLandscapeUri = settings.staticWallpaperLandscapeUri,
                    )
                }
            }
        }
        viewModelScope.launch {
            getAllPresetsUseCase().collect { presets ->
                updateState { copy(presets = presets) }
            }
        }
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                cachedMarketUser = user
                updateState { copy(isSignedIn = user != null) }
            }
        }
        viewModelScope.launch {
            getAllLiveWallpapersUseCase().collect { list ->
                updateState { copy(liveWallpapers = list) }
            }
        }
        viewModelScope.launch {
            updateState { copy(isLiveWallpaperServiceActive = wallpaperHelper.isLiveWallpaperServiceActive()) }
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
                    sendEffect(SettingsSideEffect.UploadSuccess)
                } else {
                    progress?.error?.let { error ->
                        if (error.isNetworkDisconnected()) {
                            sendEffect(SettingsSideEffect.ShowNetworkError)
                        } else {
                            sendEffect(SettingsSideEffect.UploadError(error.getErrorMessage("업로드")))
                        }
                    }
                }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeAccentColor -> changeAccentColor(intent.presetIndex)
            is SettingsIntent.SetGridImage -> setGridImage(intent.cell, intent.type, intent.uri)
            is SettingsIntent.SaveFeedSettings -> saveFeedSettings(
                intent.chzzkChannelId,
                intent.youtubeChannelId,
            )
            is SettingsIntent.SaveAppDrawerSettings -> saveAppDrawerSettings(
                intent.columns,
                intent.rows,
                intent.iconSizeRatio,
            )
            is SettingsIntent.ShowNotice -> updateState { copy(showNoticeDialog = true) }
            is SettingsIntent.DismissNotice -> dismissNotice()
            is SettingsIntent.ResetTab -> sendEffect(SettingsSideEffect.NavigateToMain)
            is SettingsIntent.SavePreset -> savePreset(intent)
            is SettingsIntent.LoadPreset -> loadPreset(intent)
            is SettingsIntent.DeletePreset -> deletePreset(intent.preset)
            is SettingsIntent.ResetAllGridImages -> resetAllGridImages()
            is SettingsIntent.SignInWithGoogle -> signInAndRetryUpload(intent.idToken)
            is SettingsIntent.UploadPreset -> uploadPreset(intent)
            is SettingsIntent.PauseUpload -> uploadProgressTracker.pause()
            is SettingsIntent.ResumeUpload -> uploadProgressTracker.resume()
            is SettingsIntent.CancelUpload -> cancelUpload()
            is SettingsIntent.LoadLiveWallpaperFile -> {
                val isLandscape = currentState.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeUri = intent.uri, selectedLiveWallpaperLandscapeId = null) }
                } else {
                    updateState { copy(selectedLiveWallpaperUri = intent.uri, selectedLiveWallpaperId = null) }
                }
            }
            is SettingsIntent.CreateLiveWallpaper -> createLiveWallpaper(intent.name)
            is SettingsIntent.SelectLiveWallpaper -> {
                val isLandscape = currentState.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeId = intent.id, selectedLiveWallpaperLandscapeUri = intent.uri) }
                } else {
                    updateState { copy(selectedLiveWallpaperId = intent.id, selectedLiveWallpaperUri = intent.uri) }
                }
            }
            is SettingsIntent.SetActiveLiveWallpaper -> setActiveLiveWallpaper(intent.id, intent.uri, intent.orientation)
            is SettingsIntent.DeleteLiveWallpaper -> deleteLiveWallpaper(intent.id)
            is SettingsIntent.ClearActiveLiveWallpaper -> clearActiveLiveWallpaper(intent.orientation)
            is SettingsIntent.SwitchOrientationTab -> updateState { copy(selectedOrientationTab = intent.orientation) }
            is SettingsIntent.CheckActiveWallpaper -> {
                updateState { copy(isLiveWallpaperServiceActive = wallpaperHelper.isLiveWallpaperServiceActive()) }
            }
            is SettingsIntent.SetStaticWallpaper -> setStaticWallpaper(intent.uri, intent.orientation, intent.isCurrentLandscape)
            is SettingsIntent.ClearStaticWallpaper -> clearStaticWallpaper(intent.orientation)
            is SettingsIntent.SwitchStaticWallpaperTab -> updateState { copy(selectedStaticWallpaperTab = intent.orientation) }
            is SettingsIntent.ApplyStaticWallpaperForOrientation -> applyStaticWallpaperForOrientation(intent.isLandscape)
        }
    }

    private fun cancelUpload() {
        uploadProgressTracker.clear()
        updateState { copy(uploadProgress = null, pendingUploadPresetName = null) }
        pendingUploadLocalPresetId = 0
        sendEffect(SettingsSideEffect.StopUploadService)
    }

    private fun signInAndRetryUpload(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { pendingUploadIntent?.let { handleIntent(it) } }
                .onFailure { error ->
                    if (error.isNetworkDisconnected()) {
                        sendEffect(SettingsSideEffect.ShowNetworkError)
                    } else {
                        sendEffect(SettingsSideEffect.UploadError(error.getErrorMessage("로그인")))
                    }
                }
        }
    }

    fun checkNotice(version: String) {
        currentNoticeVersion = version
        viewModelScope.launch {
            if (checkNoticeUseCase(version)) {
                updateState { copy(showNoticeDialog = true) }
            }
        }
    }

    private fun changeAccentColor(presetIndex: Int) {
        updateState { copy(colorPresetIndex = presetIndex) }
        viewModelScope.launch {
            saveColorPresetUseCase(presetIndex)
        }
    }

    private fun setGridImage(cell: GridCell, type: ImageType, uri: String) {
        val currentImages = currentState.gridCellImages.toMutableMap()
        val existing = currentImages[cell] ?: GridCellImage(cell)
        val updated = when (type) {
            ImageType.IDLE -> existing.copy(idleImageUri = uri)
            ImageType.EXPANDED -> existing.copy(expandedImageUri = uri)
        }
        currentImages[cell] = updated
        updateState { copy(gridCellImages = currentImages) }
        viewModelScope.launch {
            saveGridCellImageUseCase(cell, updated.idleImageUri, updated.expandedImageUri)
        }
    }

    private fun saveFeedSettings(chzzkChannelId: String, youtubeChannelId: String) {
        updateState {
            copy(
                chzzkChannelId = chzzkChannelId,
                youtubeChannelId = youtubeChannelId,
            )
        }
        viewModelScope.launch {
            runCatching {
                saveFeedSettingsUseCase(chzzkChannelId, youtubeChannelId)
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("피드 설정 저장")))
            }
        }
    }

    private fun saveAppDrawerSettings(columns: Int, rows: Int, iconSizeRatio: Float) {
        updateState {
            copy(
                appDrawerGridColumns = columns,
                appDrawerGridRows = rows,
                appDrawerIconSizeRatio = iconSizeRatio,
            )
        }
        viewModelScope.launch {
            runCatching {
                saveAppDrawerSettingsUseCase(columns, rows, iconSizeRatio)
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("앱 서랍 설정 저장")))
            }
        }
    }

    private fun dismissNotice() {
        updateState { copy(showNoticeDialog = false) }
        viewModelScope.launch {
            dismissNoticeUseCase(currentNoticeVersion)
        }
    }

    private fun savePreset(intent: SettingsIntent.SavePreset) {
        viewModelScope.launch {
            val state = currentState
            // 라이브 배경화면은 이미 filesDir에 저장된 경로이므로 별도 복사 없음
            val wallpaperPath = if (intent.saveWallpaper && intent.wallpaperUri != null && !intent.isLiveWallpaper) {
                wallpaperHelper.copyWallpaperFromUri(intent.wallpaperUri, System.currentTimeMillis())
            } else null

            val preset = Preset(
                name = intent.name,
                hasTopLeftImage = intent.saveHome && state.gridCellImages[GridCell.TOP_LEFT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasTopRightImage = intent.saveHome && state.gridCellImages[GridCell.TOP_RIGHT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasBottomLeftImage = intent.saveHome && state.gridCellImages[GridCell.BOTTOM_LEFT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                hasBottomRightImage = intent.saveHome && state.gridCellImages[GridCell.BOTTOM_RIGHT]?.let { it.idleImageUri != null || it.expandedImageUri != null } == true,
                topLeftIdleUri = if (intent.saveHome) state.gridCellImages[GridCell.TOP_LEFT]?.idleImageUri else null,
                topLeftExpandedUri = if (intent.saveHome) state.gridCellImages[GridCell.TOP_LEFT]?.expandedImageUri else null,
                topRightIdleUri = if (intent.saveHome) state.gridCellImages[GridCell.TOP_RIGHT]?.idleImageUri else null,
                topRightExpandedUri = if (intent.saveHome) state.gridCellImages[GridCell.TOP_RIGHT]?.expandedImageUri else null,
                bottomLeftIdleUri = if (intent.saveHome) state.gridCellImages[GridCell.BOTTOM_LEFT]?.idleImageUri else null,
                bottomLeftExpandedUri = if (intent.saveHome) state.gridCellImages[GridCell.BOTTOM_LEFT]?.expandedImageUri else null,
                bottomRightIdleUri = if (intent.saveHome) state.gridCellImages[GridCell.BOTTOM_RIGHT]?.idleImageUri else null,
                bottomRightExpandedUri = if (intent.saveHome) state.gridCellImages[GridCell.BOTTOM_RIGHT]?.expandedImageUri else null,
                hasFeedSettings = intent.saveFeed,
                useFeed = true,
                youtubeChannelId = if (intent.saveFeed) state.youtubeChannelId else "",
                chzzkChannelId = if (intent.saveFeed) state.chzzkChannelId else "",
                hasAppDrawerSettings = intent.saveDrawer,
                appDrawerColumns = if (intent.saveDrawer) state.appDrawerGridColumns else 4,
                appDrawerRows = if (intent.saveDrawer) state.appDrawerGridRows else 6,
                appDrawerIconSizeRatio = if (intent.saveDrawer) state.appDrawerIconSizeRatio else 1.0f,
                hasWallpaperSettings = intent.saveWallpaper,
                wallpaperUri = if (intent.isLiveWallpaper) null else wallpaperPath,
                enableParallax = false,
                hasThemeSettings = intent.saveTheme,
                themeColorHex = if (intent.saveTheme) state.colorPresetIndex.toString() else null,
                isLiveWallpaper = intent.isLiveWallpaper,
                liveWallpaperUri = if (intent.isLiveWallpaper) intent.wallpaperUri else null,
                isLiveWallpaperLandscape = intent.isLiveWallpaperLandscape,
                liveWallpaperLandscapeUri = if (intent.isLiveWallpaperLandscape) intent.wallpaperLandscapeUri else null,
            )
            runCatching { savePresetUseCase(preset) }
                .onFailure { error ->
                    if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                    else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("프리셋 저장")))
                }
        }
    }

    private fun loadPreset(intent: SettingsIntent.LoadPreset) {
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
                        sendEffect(SettingsSideEffect.LaunchLiveWallpaperPicker)
                    } else {
                        preset.wallpaperUri?.let { uri ->
                            wallpaperHelper.setWallpaperFromPreset(uri)
                        }
                    }
                    if (preset.isLiveWallpaperLandscape && preset.liveWallpaperLandscapeUri != null) {
                        setLiveWallpaperUseCase(preset.id, preset.liveWallpaperLandscapeUri, WallpaperOrientation.LANDSCAPE)
                    }
                }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("프리셋 적용")))
            }
        }
    }

    private fun resetAllGridImages() {
        val emptyImages = GridCell.entries.associateWith { GridCellImage(it) }
        updateState { copy(gridCellImages = emptyImages) }
        viewModelScope.launch {
            GridCell.entries.forEach { cell ->
                saveGridCellImageUseCase(cell, null, null)
            }
        }
    }

    private fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            runCatching {
                deletePresetUseCase(preset)
                preset.wallpaperUri?.let { wallpaperHelper.deletePresetWallpaper(it) }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("프리셋 삭제")))
            }
        }
    }

    private fun createLiveWallpaper(name: String) {
        val state = currentState
        val isLandscape = state.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
        val uri = if (isLandscape) state.selectedLiveWallpaperLandscapeUri else state.selectedLiveWallpaperUri
        uri ?: return
        viewModelScope.launch {
            runCatching {
                val lw = saveLiveWallpaperUseCase(name, uri)
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeId = lw.id, selectedLiveWallpaperLandscapeUri = lw.fileUri) }
                } else {
                    updateState { copy(selectedLiveWallpaperId = lw.id, selectedLiveWallpaperUri = lw.fileUri) }
                }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 저장")))
            }
        }
    }

    private fun setActiveLiveWallpaper(id: Int, uri: String, orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching {
                setLiveWallpaperUseCase(id, uri, orientation)
                sendEffect(SettingsSideEffect.LaunchLiveWallpaperPicker)
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 설정")))
            }
        }
    }

    private fun deleteLiveWallpaper(id: Int) {
        viewModelScope.launch {
            runCatching {
                deleteLiveWallpaperUseCase(id)
                val state = currentState
                if (state.selectedLiveWallpaperId == id) {
                    updateState { copy(selectedLiveWallpaperId = null, selectedLiveWallpaperUri = null) }
                }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 삭제")))
            }
        }
    }

    private fun clearActiveLiveWallpaper(orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching { setLiveWallpaperUseCase(null, null, orientation) }
                .onSuccess {
                    if (orientation == WallpaperOrientation.LANDSCAPE) {
                        updateState { copy(activeLandscapeWallpaperId = null) }
                    } else {
                        updateState { copy(activePortraitWallpaperId = null, isLiveWallpaperServiceActive = false) }
                    }
                    sendEffect(SettingsSideEffect.ReloadWallpaper)
                }
                .onFailure { error ->
                    if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                    else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 해제")))
                }
        }
    }

    private fun setStaticWallpaper(uri: String, orientation: WallpaperOrientation, isCurrentLandscape: Boolean) {
        viewModelScope.launch {
            runCatching {
                val filePath = setStaticWallpaperUseCase(uri, orientation) ?: return@runCatching
                // 저장한 방향이 현재 기기 방향과 일치할 때만 즉시 WallpaperManager 적용
                val savedIsLandscape = orientation == WallpaperOrientation.LANDSCAPE
                if (filePath.isNotEmpty() && savedIsLandscape == isCurrentLandscape) {
                    wallpaperHelper.setWallpaperFromPreset(filePath)
                }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("배경화면 설정")))
            }
        }
    }

    private fun clearStaticWallpaper(orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching {
                setStaticWallpaperUseCase(null, orientation)
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(SettingsSideEffect.ShowNetworkError)
                else sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("배경화면 초기화")))
            }
        }
    }

    private fun applyStaticWallpaperForOrientation(isLandscape: Boolean) {
        if (wallpaperHelper.isLiveWallpaperServiceActive()) return
        val state = currentState
        val filePath = if (isLandscape) {
            state.staticWallpaperLandscapeUri ?: state.staticWallpaperPortraitUri
        } else {
            state.staticWallpaperPortraitUri
        } ?: return
        viewModelScope.launch {
            runCatching { wallpaperHelper.setWallpaperFromPreset(filePath) }
        }
    }

    private fun uploadPreset(intent: SettingsIntent.UploadPreset) {
        if (cachedMarketUser == null) {
            pendingUploadIntent = intent
            sendEffect(SettingsSideEffect.RequireSignIn)
            return
        }
        pendingUploadIntent = null

        val p = intent.preset
        pendingUploadLocalPresetId = p.id

        // 데이터 홀더에 로컬 preset + 메타데이터 저장 후 서비스 시작 요청
        uploadDataHolder.pendingPreset = p
        uploadDataHolder.pendingPreviewUris = intent.previewUris
        uploadDataHolder.pendingDescription = intent.description
        uploadDataHolder.pendingTags = intent.tags

        updateState { copy(pendingUploadPresetName = p.name) }

        sendEffect(SettingsSideEffect.StartUploadService(p.name))
        sendEffect(SettingsSideEffect.UploadStarted(p.name))
    }
}
