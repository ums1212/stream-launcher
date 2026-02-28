package org.comon.streamlauncher.domain.model

data class LauncherSettings(
    val colorPresetIndex: Int = 0,
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
    val cellAssignments: Map<GridCell, List<String>> = emptyMap(),
    val chzzkChannelId: String = "",
    val youtubeChannelId: String = "",
    val wallpaperImage: String? = null,
    val appDrawerGridColumns: Int = 4,
    val appDrawerGridRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,
)
