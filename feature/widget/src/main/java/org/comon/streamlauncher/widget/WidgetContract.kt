package org.comon.streamlauncher.widget

import org.comon.streamlauncher.domain.model.WidgetPlacement
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class WidgetState(
    val placements: List<WidgetPlacement> = emptyList(),
    val isEditMode: Boolean = false,
    val draggingWidgetId: Int? = null,
    val resizingWidgetId: Int? = null,
    val dragPreviewCol: Int = 0,
    val dragPreviewRow: Int = 0,
    val resizePreviewColSpan: Int = 1,
    val resizePreviewRowSpan: Int = 1,
) : UiState

sealed interface WidgetIntent : UiIntent {
    data class AddWidget(val appWidgetId: Int, val minCols: Int, val minRows: Int) : WidgetIntent
    data class RemoveWidget(val appWidgetId: Int) : WidgetIntent
    data class StartDrag(val appWidgetId: Int) : WidgetIntent
    data class UpdateDrag(val gridCol: Int, val gridRow: Int) : WidgetIntent
    data object EndDrag : WidgetIntent
    data class StartResize(val appWidgetId: Int) : WidgetIntent
    data class UpdateResize(val colSpan: Int, val rowSpan: Int) : WidgetIntent
    data object EndResize : WidgetIntent
    data class SetEditMode(val isEdit: Boolean) : WidgetIntent
}

sealed interface WidgetSideEffect : UiSideEffect
