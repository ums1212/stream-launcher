package org.comon.streamlauncher.launcher

import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.launcher.model.GridCell
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class HomeState(
    val expandedCell: GridCell? = null,
    val appsInCells: Map<GridCell, List<AppEntity>> = emptyMap(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredApps: List<AppEntity> = emptyList(),
) : UiState

sealed interface HomeIntent : UiIntent {
    data object LoadApps : HomeIntent
    data object ResetHome : HomeIntent
    data class ClickGrid(val cell: GridCell) : HomeIntent
    data class ClickApp(val app: AppEntity) : HomeIntent
    data class Search(val query: String) : HomeIntent
}

sealed interface HomeSideEffect : UiSideEffect {
    data class NavigateToApp(val packageName: String) : HomeSideEffect
    data class ShowError(val message: String) : HomeSideEffect
}
