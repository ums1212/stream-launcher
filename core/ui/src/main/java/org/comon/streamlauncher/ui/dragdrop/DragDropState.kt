package org.comon.streamlauncher.ui.dragdrop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell

class DragDropState {
    var draggedApp: AppEntity? by mutableStateOf(null)
    var dragOffset: Offset by mutableStateOf(Offset.Zero)
    var hoveredCell: GridCell? by mutableStateOf(null)
    val isDragging: Boolean get() = draggedApp != null

    private val cellBounds = mutableMapOf<GridCell, Rect>()
    var onScrollToHome: (() -> Unit)? = null

    /** 어떤 셀에도 호버하지 않은 상태 (Cancel Zone) */
    val isInCancelZone: Boolean get() = isDragging && hoveredCell == null

    fun startDrag(app: AppEntity, position: Offset) {
        draggedApp = app
        dragOffset = position
        hoveredCell = null
        onScrollToHome?.invoke()
    }

    fun updateDrag(position: Offset) {
        dragOffset = position
        hoveredCell = cellBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(position)
        }?.key
    }

    fun endDrag(): Pair<AppEntity, GridCell>? {
        val app = draggedApp
        val cell = hoveredCell
        cancelDrag()
        return if (app != null && cell != null) Pair(app, cell) else null
    }

    fun cancelDrag() {
        draggedApp = null
        dragOffset = Offset.Zero
        hoveredCell = null
    }

    fun registerCellBounds(cell: GridCell, rect: Rect) {
        cellBounds[cell] = rect
    }

    fun unregisterCellBounds(cell: GridCell) {
        cellBounds.remove(cell)
    }
}

val LocalDragDropState = staticCompositionLocalOf { DragDropState() }
