package org.comon.streamlauncher.settings.image

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ImageSettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
) : BaseViewModel<ImageSettingsState, ImageSettingsIntent, ImageSettingsSideEffect>(ImageSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState { copy(gridCellImages = settings.gridCellImages) }
            }
        }
    }

    override fun handleIntent(intent: ImageSettingsIntent) {
        when (intent) {
            is ImageSettingsIntent.SetGridImage -> setGridImage(intent.cell, intent.type, intent.uri)
            is ImageSettingsIntent.ResetAllGridImages -> resetAllGridImages()
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

    private fun resetAllGridImages() {
        val emptyImages = GridCell.entries.associateWith { GridCellImage(it) }
        updateState { copy(gridCellImages = emptyImages) }
        viewModelScope.launch {
            GridCell.entries.forEach { cell ->
                saveGridCellImageUseCase(cell, null, null)
            }
        }
    }
}
