package org.comon.streamlauncher.settings.image

import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class ImageSettingsState(
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
) : UiState

sealed interface ImageSettingsIntent : UiIntent {
    data class SetGridImage(val cell: GridCell, val type: ImageType, val uri: String) : ImageSettingsIntent
    data object ResetAllGridImages : ImageSettingsIntent
}

sealed interface ImageSettingsSideEffect : UiSideEffect
