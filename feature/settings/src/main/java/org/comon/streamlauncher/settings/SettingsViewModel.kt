package org.comon.streamlauncher.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DeletePresetUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetAllPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.usecase.SavePresetUseCase
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.settings.model.SettingsTab
import org.comon.streamlauncher.ui.BaseViewModel
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
) : BaseViewModel<SettingsState, SettingsIntent, SettingsSideEffect>(SettingsState()) {

    private var currentNoticeVersion: String = ""

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
                    )
                }
            }
        }
        viewModelScope.launch {
            getAllPresetsUseCase().collect { presets ->
                updateState { copy(presets = presets) }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeTab -> updateState { copy(currentTab = intent.tab) }
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
            is SettingsIntent.ResetTab -> updateState { copy(currentTab = SettingsTab.MAIN) }
            is SettingsIntent.SavePreset -> savePreset(intent)
            is SettingsIntent.LoadPreset -> loadPreset(intent)
            is SettingsIntent.DeletePreset -> deletePreset(intent.preset)
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
            saveFeedSettingsUseCase(chzzkChannelId, youtubeChannelId)
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
            saveAppDrawerSettingsUseCase(columns, rows, iconSizeRatio)
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
            // For wallpaper, try to save it if requested
            val wallpaperPath = if (intent.saveWallpaper) {
                wallpaperHelper.saveCurrentWallpaperForPreset(System.currentTimeMillis())
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
                useFeed = true, // Default to true if feed settings saved
                youtubeChannelId = if (intent.saveFeed) state.youtubeChannelId else "",
                youtubeChannelName = "", 
                chzzkChannelId = if (intent.saveFeed) state.chzzkChannelId else "",
                hasAppDrawerSettings = intent.saveDrawer,
                appDrawerColumns = if (intent.saveDrawer) state.appDrawerGridColumns else 4,
                appDrawerRows = if (intent.saveDrawer) state.appDrawerGridRows else 6,
                appDrawerIconSizeRatio = if (intent.saveDrawer) state.appDrawerIconSizeRatio else 1.0f,
                hasWallpaperSettings = intent.saveWallpaper,
                wallpaperUri = wallpaperPath,
                enableParallax = false,
                hasThemeSettings = intent.saveTheme,
                themeColorHex = if (intent.saveTheme) state.colorPresetIndex.toString() else null // Store index in hex field for simplicity
            )
            savePresetUseCase(preset)
        }
    }

    private fun loadPreset(intent: SettingsIntent.LoadPreset) {
        viewModelScope.launch {
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
                preset.wallpaperUri?.let { uri -> 
                    wallpaperHelper.setWallpaperFromPreset(uri)
                }
            }
        }
    }

    private fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            deletePresetUseCase(preset)
            preset.wallpaperUri?.let { wallpaperHelper.deletePresetWallpaper(it) }
        }
    }
}
