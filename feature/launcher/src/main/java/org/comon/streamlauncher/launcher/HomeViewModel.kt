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
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.domain.usecase.GetInstalledAppsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.util.ChosungMatcher
import org.comon.streamlauncher.domain.model.GridCell
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
                    )
                }
            }
        }
        // 검색어 디바운스
        viewModelScope.launch {
            _searchQuery
                .debounce(100)
                .collect { query ->
                    val allApps = currentState.appsInCells.values.flatten().sortedBy { it.label }
                    updateState { copy(filteredApps = filterApps(allApps, query)) }
                }
        }
        handleIntent(HomeIntent.LoadApps)
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadApps -> loadApps()
            is HomeIntent.ResetHome -> resetHome()
            is HomeIntent.ClickGrid -> toggleCell(intent.cell)
            is HomeIntent.ClickApp -> {
                sendEffect(HomeSideEffect.NavigateToApp(intent.app.packageName))
                resetHome()
            }
            is HomeIntent.Search -> search(intent.query)
            is HomeIntent.ChangeSettingsTab -> updateState { copy(currentSettingsTab = intent.tab) }
            is HomeIntent.ChangeAccentColor -> changeAccentColor(intent.presetIndex)
            is HomeIntent.SetGridImage -> setGridImage(intent.cell, intent.type, intent.uri)
        }
    }

    private fun loadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                getInstalledAppsUseCase().collect { apps ->
                    // 런처 액티비티 복수 등록 앱(Google Search 등) 중복 제거 — packageName 기준 첫 번째 항목 유지
                    val unique = apps.distinctBy { it.packageName }
                    val allSorted = unique.sortedBy { it.label }
                    updateState {
                        copy(
                            appsInCells = distributeApps(unique),
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
                filteredApps = appsInCells.values.flatten().sortedBy { it.label },
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

    private fun filterApps(apps: List<AppEntity>, query: String): List<AppEntity> {
        if (query.isEmpty()) return apps
        return apps.filter { ChosungMatcher.matchesChosung(it.label, query) }
    }

    private fun distributeApps(apps: List<AppEntity>): Map<GridCell, List<AppEntity>> {
        val sorted = apps.sortedBy { it.label }
        val chunkSize = (sorted.size + 3) / 4  // 올림 나눗셈
        val chunks = if (chunkSize == 0) {
            List(4) { emptyList() }
        } else {
            val chunked = sorted.chunked(chunkSize)
            // 4개가 되도록 빈 리스트 패딩
            chunked + List(maxOf(0, 4 - chunked.size)) { emptyList() }
        }
        return GridCell.entries.zip(chunks).toMap()
    }
}
