package org.comon.streamlauncher.domain.model

data class WidgetPlacement(
    val appWidgetId: Int,
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val minColumnSpan: Int = 1,
    val minRowSpan: Int = 1,
)
