package org.comon.streamlauncher.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

private val MIN_BASE_ICON_SIZE = 36.dp
private val MAX_BASE_ICON_SIZE = 64.dp
private const val ICON_WIDTH_RATIO = 0.6f
private const val ICON_HEIGHT_RATIO = 0.45f
private const val MAX_HOME_GRID_COLUMNS = 6
private const val MAX_HOME_GRID_ROWS = 6

@Immutable
data class AppGridMetrics(
    val columns: Int,
    val rows: Int,
    val capacity: Int,
    val itemWidth: Dp,
    val itemHeight: Dp,
    val iconSize: Dp,
    val textWidth: Dp,
)

fun calculateFixedAppGridMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    columns: Int,
    rows: Int,
    iconSizeRatio: Float,
): AppGridMetrics {
    val safeColumns = columns.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val safeRatio = iconSizeRatio.coerceAtLeast(0.1f)
    val itemWidth = maxWidth / safeColumns
    val itemHeight = maxHeight / safeRows
    val baseIconSize = minOf(
        MAX_BASE_ICON_SIZE,
        minOf(itemWidth * ICON_WIDTH_RATIO, itemHeight * ICON_HEIGHT_RATIO),
    ).coerceAtLeast(MIN_BASE_ICON_SIZE)

    return AppGridMetrics(
        columns = safeColumns,
        rows = safeRows,
        capacity = safeColumns * safeRows,
        itemWidth = itemWidth,
        itemHeight = itemHeight,
        iconSize = baseIconSize * safeRatio,
        textWidth = itemWidth * 0.9f,
    )
}

fun calculateAdaptiveHomeGridMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    iconSizeRatio: Float,
): AppGridMetrics {
    val safeRatio = iconSizeRatio.coerceAtLeast(0.1f)
    var bestMetrics: AppGridMetrics? = null

    for (rows in 1..MAX_HOME_GRID_ROWS) {
        for (columns in 1..MAX_HOME_GRID_COLUMNS) {
            val candidate = calculateFixedAppGridMetrics(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                columns = columns,
                rows = rows,
                iconSizeRatio = safeRatio,
            )
            if (!candidate.canFitHomeSlot(safeRatio)) continue

            val currentBest = bestMetrics
            if (
                currentBest == null ||
                candidate.capacity > currentBest.capacity ||
                (candidate.capacity == currentBest.capacity && candidate.iconSize > currentBest.iconSize)
            ) {
                bestMetrics = candidate
            }
        }
    }

    return bestMetrics ?: calculateFixedAppGridMetrics(
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        columns = 1,
        rows = 1,
        iconSizeRatio = safeRatio,
    )
}

private fun AppGridMetrics.canFitHomeSlot(iconSizeRatio: Float): Boolean {
    val requiredIconSize = MIN_BASE_ICON_SIZE * iconSizeRatio
    return itemWidth * ICON_WIDTH_RATIO >= requiredIconSize &&
        itemHeight * ICON_HEIGHT_RATIO >= requiredIconSize
}
