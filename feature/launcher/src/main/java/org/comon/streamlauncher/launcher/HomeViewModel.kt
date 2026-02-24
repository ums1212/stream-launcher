package org.comon.streamlauncher.launcher

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.usecase.GetInstalledAppsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveCellAssignmentUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.usecase.CheckFirstLaunchUseCase
import org.comon.streamlauncher.domain.usecase.SetFirstLaunchUseCase
import org.comon.streamlauncher.domain.usecase.SaveWallpaperImageUseCase
import org.comon.streamlauncher.domain.util.ChosungMatcher
import org.comon.streamlauncher.launcher.model.ImageType
import org.comon.streamlauncher.launcher.model.SettingsTab
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
    private val saveCellAssignmentUseCase: SaveCellAssignmentUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
    private val checkFirstLaunchUseCase: CheckFirstLaunchUseCase,
    private val setFirstLaunchUseCase: SetFirstLaunchUseCase,
    private val saveWallpaperImageUseCase: SaveWallpaperImageUseCase,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(HomeState()) {

    private var loadJob: Job? = null
    private val _searchQuery = MutableStateFlow("")

    init {
        // 저장된 설정 복원
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        colorPresetIndex = settings.colorPresetIndex,
                        gridCellImages = settings.gridCellImages,
                        cellAssignments = settings.cellAssignments,
                        chzzkChannelId = settings.chzzkChannelId,
                        youtubeChannelId = settings.youtubeChannelId,
                        rssUrl = settings.rssUrl,
                        wallpaperImage = settings.wallpaperImage,
                    )
                }
            }
        }
        // 검색어 디바운스
        viewModelScope.launch {
            _searchQuery
                .debounce(100)
                .collect { query ->
                    updateState { copy(filteredApps = filterApps(allApps, query)) }
                }
        }
        handleIntent(HomeIntent.LoadApps)
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadApps -> loadApps()
            is HomeIntent.ResetHome -> resetHome()
            is HomeIntent.CheckFirstLaunch -> checkFirstLaunch()
            is HomeIntent.ClickGrid -> toggleCell(intent.cell)
            is HomeIntent.ClickApp -> {
                sendEffect(HomeSideEffect.NavigateToApp(intent.app.packageName))
                resetHome()
            }
            is HomeIntent.Search -> search(intent.query)
            is HomeIntent.ChangeSettingsTab -> updateState { copy(currentSettingsTab = intent.tab) }
            is HomeIntent.ChangeAccentColor -> changeAccentColor(intent.presetIndex)
            is HomeIntent.SetGridImage -> setGridImage(intent.cell, intent.type, intent.uri)
            is HomeIntent.AssignAppToCell -> assignAppToCell(intent.app, intent.cell)
            is HomeIntent.UnassignApp -> unassignApp(intent.app)
            is HomeIntent.SaveFeedSettings -> saveFeedSettings(intent.chzzkChannelId, intent.youtubeChannelId, intent.rssUrl)
            is HomeIntent.SetWallpaperImage -> setWallpaperImage(intent.uri)
        }
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            if (checkFirstLaunchUseCase()) {
                sendEffect(HomeSideEffect.NavigateToHomeSettings)
                setFirstLaunchUseCase()
            }
        }
    }

    private fun loadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                getInstalledAppsUseCase().collect { apps ->
                    val unique = apps.distinctBy { it.packageName }
                    val allSorted = unique.sortedBy { it.label }
                    updateState {
                        copy(
                            allApps = allSorted,
                            appsInCells = distributeApps(allSorted, cellAssignments),
                            filteredApps = filterApps(allSorted, searchQuery),
                            isLoading = false,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                sendEffect(HomeSideEffect.ShowError(e.message ?: "앱 목록을 불러오는 중 오류가 발생했습니다."))
            }
        }
    }

    private fun toggleCell(cell: GridCell) {
        updateState {
            copy(expandedCell = if (expandedCell == cell) null else cell)
        }
    }

    private fun search(query: String) {
        _searchQuery.value = query
        updateState { copy(searchQuery = query) }
    }

    private fun resetHome() {
        _searchQuery.value = ""
        updateState {
            copy(
                expandedCell = null,
                searchQuery = "",
                filteredApps = allApps,
                currentSettingsTab = SettingsTab.MAIN,
            )
        }
    }

    private fun changeAccentColor(presetIndex: Int) {
        updateState { copy(colorPresetIndex = presetIndex) }
        viewModelScope.launch {
            saveColorPresetUseCase(presetIndex)
        }
    }

    private fun setGridImage(cell: GridCell, type: ImageType, uri: String) {
        val currentImages = currentState.gridCellImages.toMutableMap()
        val existing = currentImages[cell] ?: GridCellImage(cell)
        val updated = when (type) {
            ImageType.IDLE -> existing.copy(idleImageUri = uri)
            ImageType.EXPANDED -> existing.copy(expandedImageUri = uri)
        }
        currentImages[cell] = updated
        updateState { copy(gridCellImages = currentImages) }
        viewModelScope.launch {
            saveGridCellImageUseCase(cell, updated.idleImageUri, updated.expandedImageUri)
        }
    }

    private fun assignAppToCell(app: AppEntity, cell: GridCell) {
        val pkg = app.packageName
        // 다른 셀의 할당에서 해당 앱 제거 후 대상 셀에 추가
        val newAssignments = currentState.cellAssignments.toMutableMap()
        GridCell.entries.forEach { c ->
            val current = newAssignments[c]?.toMutableList() ?: mutableListOf()
            current.remove(pkg)
            newAssignments[c] = current
        }
        val targetList = newAssignments[cell]?.toMutableList() ?: mutableListOf()
        if (targetList.size >= MAX_APPS_PER_CELL) return
        if (!targetList.contains(pkg)) {
            targetList.add(pkg)
        }
        newAssignments[cell] = targetList

        updateState {
            copy(
                cellAssignments = newAssignments,
                appsInCells = distributeApps(allApps, newAssignments),
            )
        }
        // DataStore에 변경된 셀들 저장
        viewModelScope.launch {
            GridCell.entries.forEach { c ->
                saveCellAssignmentUseCase(c, newAssignments[c] ?: emptyList())
            }
        }
    }

    private fun unassignApp(app: AppEntity) {
        val pkg = app.packageName
        val newAssignments = currentState.cellAssignments.toMutableMap()
        GridCell.entries.forEach { c ->
            val current = newAssignments[c]?.toMutableList() ?: mutableListOf()
            current.remove(pkg)
            newAssignments[c] = current
        }

        updateState {
            copy(
                cellAssignments = newAssignments,
                appsInCells = distributeApps(allApps, newAssignments),
            )
        }
        viewModelScope.launch {
            GridCell.entries.forEach { c ->
                saveCellAssignmentUseCase(c, newAssignments[c] ?: emptyList())
            }
        }
    }

    private fun saveFeedSettings(chzzkChannelId: String, youtubeChannelId: String, rssUrl: String) {
        updateState {
            copy(
                chzzkChannelId = chzzkChannelId,
                youtubeChannelId = youtubeChannelId,
                rssUrl = rssUrl,
            )
        }
        viewModelScope.launch {
            saveFeedSettingsUseCase(chzzkChannelId, youtubeChannelId, rssUrl)
        }
    }

    private fun setWallpaperImage(uri: String?) {
        updateState { copy(wallpaperImage = uri) }
        viewModelScope.launch {
            saveWallpaperImageUseCase(uri)
        }
    }

    private fun filterApps(apps: List<AppEntity>, query: String): List<AppEntity> {
        if (query.isEmpty()) return apps
        return apps.filter { ChosungMatcher.matchesChosung(it.label, query) }
    }

    /**
     * 사용자가 직접 배치(핀 고정)한 앱만 각 셀에 배치.
     * 자동 배분 없이 cellAssignments 기반으로만 구성.
     */
    internal fun distributeApps(
        apps: List<AppEntity>,
        cellAssignments: Map<GridCell, List<String>> = emptyMap(),
    ): Map<GridCell, List<AppEntity>> {
        return GridCell.entries.associateWith { cell ->
            val pinnedPackageNames = cellAssignments[cell] ?: emptyList()
            pinnedPackageNames
                .mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                .sortedBy { it.label }
        }
    }

    companion object {
        const val MAX_APPS_PER_CELL = 6
    }
}
