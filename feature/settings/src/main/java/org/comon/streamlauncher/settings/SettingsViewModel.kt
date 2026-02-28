package org.comon.streamlauncher.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
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
}
