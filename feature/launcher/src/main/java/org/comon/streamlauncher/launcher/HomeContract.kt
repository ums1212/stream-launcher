package org.comon.streamlauncher.launcher

import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class HomeState(
    val expandedCell: GridCell? = null,
    val editingCell: GridCell? = null,
    val allApps: List<AppEntity> = emptyList(),
    val appsInCells: Map<GridCell, List<AppEntity>> = emptyMap(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredApps: List<AppEntity> = emptyList(),
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
    val cellAssignments: Map<GridCell, List<String>> = emptyMap(),
    val appDrawerGridColumns: Int = 4,
    val appDrawerGridRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,
) : UiState {
    val pinnedPackages: Set<String> get() = cellAssignments.values.flatten().toSet()
}

sealed interface HomeIntent : UiIntent {
    data object LoadApps : HomeIntent
    data object ResetHome : HomeIntent
    data object CheckFirstLaunch : HomeIntent
    data class ClickGrid(val cell: GridCell) : HomeIntent
    data class ClickApp(val app: AppEntity) : HomeIntent
    data class Search(val query: String) : HomeIntent
    data class AssignAppToCell(val app: AppEntity, val cell: GridCell) : HomeIntent
    data class UnassignApp(val app: AppEntity) : HomeIntent
    data class SetEditingCell(val cell: GridCell?) : HomeIntent
    data class MoveAppInCell(val cell: GridCell, val fromIndex: Int, val toIndex: Int) : HomeIntent
    data class MoveAppBetweenCells(val app: AppEntity, val fromCell: GridCell, val toCell: GridCell, val toIndex: Int = -1) : HomeIntent
}

sealed interface HomeSideEffect : UiSideEffect {
    data class NavigateToApp(val packageName: String) : HomeSideEffect
    data class ShowError(val message: String) : HomeSideEffect
    data object SetDefaultHomeApp : HomeSideEffect
}
