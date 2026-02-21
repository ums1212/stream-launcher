package org.comon.streamlauncher.launcher

import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.launcher.model.SettingsTab
import org.comon.streamlauncher.ui.UiIntent
import org.comon.streamlauncher.ui.UiSideEffect
import org.comon.streamlauncher.ui.UiState

data class HomeState(
    val expandedCell: GridCell? = null,
    val appsInCells: Map<GridCell, List<AppEntity>> = emptyMap(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredApps: List<AppEntity> = emptyList(),
    val currentSettingsTab: SettingsTab = SettingsTab.MAIN,
    val gridCellImages: Map<GridCell, GridCellImage> = GridCell.entries.associateWith { GridCellImage(it) },
) : UiState

sealed interface HomeIntent : UiIntent {
    data object LoadApps : HomeIntent
    data object ResetHome : HomeIntent
    data class ClickGrid(val cell: GridCell) : HomeIntent
    data class ClickApp(val app: AppEntity) : HomeIntent
    data class Search(val query: String) : HomeIntent
    data class ChangeSettingsTab(val tab: SettingsTab) : HomeIntent
}

sealed interface HomeSideEffect : UiSideEffect {
    data class NavigateToApp(val packageName: String) : HomeSideEffect
    data class ShowError(val message: String) : HomeSideEffect
}
