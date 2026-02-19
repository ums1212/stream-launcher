package org.comon.streamlauncher.launcher

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.usecase.GetInstalledAppsUseCase
import org.comon.streamlauncher.launcher.model.GridCell
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(HomeState()) {

    private var loadJob: Job? = null

    init {
        handleIntent(HomeIntent.LoadApps)
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadApps -> loadApps()
            is HomeIntent.ClickGrid -> toggleCell(intent.cell)
            is HomeIntent.ClickApp -> sendEffect(HomeSideEffect.NavigateToApp(intent.app.packageName))
        }
    }

    private fun loadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                getInstalledAppsUseCase().collect { apps ->
                    updateState {
                        copy(
                            appsInCells = distributeApps(apps),
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
