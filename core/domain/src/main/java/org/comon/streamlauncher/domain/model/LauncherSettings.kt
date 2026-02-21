package org.comon.streamlauncher.domain.model

data class LauncherSettings(
    val colorPresetIndex: Int = 0,
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
    val cellAssignments: Map<GridCell, List<String>> = emptyMap(),
)
