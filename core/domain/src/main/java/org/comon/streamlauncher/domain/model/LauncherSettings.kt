package org.comon.streamlauncher.domain.model

data class LauncherSettings(
    val colorPresetIndex: Int = 0,
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
    val cellAssignments: Map<GridCell, List<String>> = emptyMap(),
    val chzzkChannelId: String = "d2fb83a5db130bf4d273c981b82ca41f",
    val rssUrl: String = "",
    val youtubeChannelId: String = "",
    val wallpaperImage: String? = null,
)
