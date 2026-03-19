package org.comon.streamlauncher.widget

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.WidgetPlacement
import org.comon.streamlauncher.domain.repository.WidgetRepository
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository,
) : BaseViewModel<WidgetState, WidgetIntent, WidgetSideEffect>(WidgetState()) {

    init {
        viewModelScope.launch { widgetRepository.migrateLegacySlots() }
        viewModelScope.launch {
            widgetRepository.getWidgetPlacements().collect { placements ->
                updateState { copy(placements = placements) }
            }
        }
    }

    override fun handleIntent(intent: WidgetIntent) {
        when (intent) {
            is WidgetIntent.AddWidget -> addWidget(intent.appWidgetId, intent.minCols, intent.minRows)
            is WidgetIntent.RemoveWidget -> removeWidget(intent.appWidgetId)
            is WidgetIntent.StartDrag -> startDrag(intent.appWidgetId)
            is WidgetIntent.UpdateDrag -> updateDrag(intent.gridCol, intent.gridRow)
            is WidgetIntent.EndDrag -> endDrag()
            is WidgetIntent.StartResize -> startResize(intent.appWidgetId)
            is WidgetIntent.UpdateResize -> updateResize(intent.colSpan, intent.rowSpan)
            is WidgetIntent.EndResize -> endResize()
            is WidgetIntent.SetEditMode -> updateState { copy(isEditMode = intent.isEdit) }
        }
    }

    private fun addWidget(appWidgetId: Int, minCols: Int, minRows: Int) {
        viewModelScope.launch {
            val placements = currentState.placements
            val (col, row) = findAvailablePosition(placements, minCols, minRows)
            val placement = WidgetPlacement(
                appWidgetId = appWidgetId,
                column = col,
                row = row,
                columnSpan = minCols,
                rowSpan = minRows,
                minColumnSpan = minCols,
                minRowSpan = minRows,
            )
            widgetRepository.addWidget(placement)
        }
    }

    private fun removeWidget(appWidgetId: Int) {
        viewModelScope.launch { widgetRepository.removeWidget(appWidgetId) }
    }

    private fun startDrag(appWidgetId: Int) {
        val placement = currentState.placements.find { it.appWidgetId == appWidgetId } ?: return
        updateState {
            copy(
                draggingWidgetId = appWidgetId,
                dragPreviewCol = placement.column,
                dragPreviewRow = placement.row,
            )
        }
    }

    private fun updateDrag(gridCol: Int, gridRow: Int) {
        if (currentState.draggingWidgetId == null) return
        updateState { copy(dragPreviewCol = gridCol, dragPreviewRow = gridRow) }
    }

    private fun endDrag() {
        val state = currentState
        val draggingId = state.draggingWidgetId ?: return
        viewModelScope.launch {
            widgetRepository.updateWidgetPlacement(draggingId, state.dragPreviewCol, state.dragPreviewRow)
        }
        updateState { copy(draggingWidgetId = null) }
    }

    private fun startResize(appWidgetId: Int) {
        val placement = currentState.placements.find { it.appWidgetId == appWidgetId } ?: return
        updateState {
            copy(
                resizingWidgetId = appWidgetId,
                resizePreviewColSpan = placement.columnSpan,
                resizePreviewRowSpan = placement.rowSpan,
            )
        }
    }

    private fun updateResize(colSpan: Int, rowSpan: Int) {
        if (currentState.resizingWidgetId == null) return
        updateState { copy(resizePreviewColSpan = colSpan, resizePreviewRowSpan = rowSpan) }
    }

    private fun endResize() {
        val state = currentState
        val resizingId = state.resizingWidgetId ?: return
        viewModelScope.launch {
            widgetRepository.updateWidgetSize(resizingId, state.resizePreviewColSpan, state.resizePreviewRowSpan)
        }
        updateState { copy(resizingWidgetId = null) }
    }

    /**
     * 행 우선 탐색으로 [colSpan × rowSpan] 위젯이 들어갈 빈 위치를 찾는다.
     * 기본 그리드 5×10 기준 탐색; 빈 자리 없으면 기존 위젯 아래에 추가.
     */
    private fun findAvailablePosition(
        placements: List<WidgetPlacement>,
        colSpan: Int,
        rowSpan: Int,
        gridCols: Int = 5,
        gridRows: Int = 10,
    ): Pair<Int, Int> {
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                if (col + colSpan > gridCols) continue
                if (row + rowSpan > gridRows) continue
                if (!overlapsAny(placements, col, row, colSpan, rowSpan)) return col to row
            }
        }
        val maxRow = placements.maxOfOrNull { it.row + it.rowSpan } ?: 0
        return 0 to maxRow
    }

    private fun overlapsAny(
        placements: List<WidgetPlacement>,
        col: Int, row: Int, colSpan: Int, rowSpan: Int,
    ): Boolean = placements.any { p ->
        col < p.column + p.columnSpan &&
            col + colSpan > p.column &&
            row < p.row + p.rowSpan &&
            row + rowSpan > p.row
    }
}
