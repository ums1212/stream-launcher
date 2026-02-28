package org.comon.streamlauncher.ui.dragdrop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell

/**
 * 드래그 완료 결과.
 * @param sourceCell null이면 앱 드로어에서 드래그한 것
 * @param sourceIndex -1이면 앱 드로어에서 드래그한 것
 * @param targetSlotIndex -1이면 슬롯 정밀 정보 없음
 */
data class DragResult(
    val app: AppEntity,
    val targetCell: GridCell,
    val targetSlotIndex: Int,
    val sourceCell: GridCell?,
    val sourceIndex: Int,
)

class DragDropState {
    var draggedApp: AppEntity? by mutableStateOf(null)
    var dragOffset: Offset by mutableStateOf(Offset.Zero)
    var hoveredCell: GridCell? by mutableStateOf(null)
    var hoveredSlotIndex: Int by mutableIntStateOf(-1)
    var dragSourceCell: GridCell? by mutableStateOf(null)
    var dragSourceIndex: Int by mutableIntStateOf(-1)

    val isDragging: Boolean get() = draggedApp != null

    private val cellBounds = mutableMapOf<GridCell, Rect>()
    private val slotBounds = mutableMapOf<Pair<GridCell, Int>, Rect>()
    var onScrollToHome: (() -> Unit)? = null

    /** 어떤 셀에도 호버하지 않은 상태 (Cancel Zone) */
    val isInCancelZone: Boolean get() = isDragging && hoveredCell == null

    fun startDrag(
        app: AppEntity,
        position: Offset,
        sourceCell: GridCell? = null,
        sourceIndex: Int = -1,
    ) {
        draggedApp = app
        dragOffset = position
        hoveredCell = null
        hoveredSlotIndex = -1
        dragSourceCell = sourceCell
        dragSourceIndex = sourceIndex
        onScrollToHome?.invoke()
    }

    fun updateDrag(position: Offset) {
        dragOffset = position
        hoveredCell = cellBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(position)
        }?.key

        val currentCell = hoveredCell
        hoveredSlotIndex = if (currentCell != null) {
            slotBounds.entries
                .filter { (key, _) -> key.first == currentCell }
                .firstOrNull { (_, rect) -> rect.contains(position) }
                ?.key?.second ?: -1
        } else {
            -1
        }
    }

    fun endDrag(): DragResult? {
        val app = draggedApp ?: return null
        val targetCell = hoveredCell
        val targetSlot = hoveredSlotIndex
        val sourceCell = dragSourceCell
        val sourceIndex = dragSourceIndex
        cancelDrag()
        return if (targetCell != null) {
            DragResult(app, targetCell, targetSlot, sourceCell, sourceIndex)
        } else null
    }

    fun cancelDrag() {
        draggedApp = null
        dragOffset = Offset.Zero
        hoveredCell = null
        hoveredSlotIndex = -1
        dragSourceCell = null
        dragSourceIndex = -1
    }

    fun registerCellBounds(cell: GridCell, rect: Rect) {
        cellBounds[cell] = rect
    }

    fun unregisterCellBounds(cell: GridCell) {
        cellBounds.remove(cell)
    }

    fun registerSlotBounds(cell: GridCell, index: Int, rect: Rect) {
        slotBounds[Pair(cell, index)] = rect
    }

    fun unregisterSlotBounds(cell: GridCell, index: Int) {
        slotBounds.remove(Pair(cell, index))
    }
}

val LocalDragDropState = staticCompositionLocalOf { DragDropState() }
