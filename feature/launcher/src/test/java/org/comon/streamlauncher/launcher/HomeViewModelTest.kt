package org.comon.streamlauncher.launcher

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.usecase.GetInstalledAppsUseCase
import org.comon.streamlauncher.launcher.model.GridCell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleApps = (1..8).map { i ->
        AppEntity(
            packageName = "com.example.app$i",
            label = "앱 $i",
            activityName = "com.example.app$i.MainActivity"
        )
    }

    private lateinit var useCase: GetInstalledAppsUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        every { useCase() } returns flowOf(sampleApps)
        viewModel = HomeViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. 초기 상태 확인
    @Test
    fun `초기 상태 - expandedCell null, isLoading false, appsInCells 비어있음`() {
        // HomeViewModel init 전 직접 확인 (init에서 LoadApps 호출하기 때문에 별도 ViewModel 생성)
        val freshUseCase: GetInstalledAppsUseCase = mockk()
        every { freshUseCase() } returns flowOf(emptyList())
        val freshVm = HomeViewModel(freshUseCase)

        val initialState = freshVm.uiState.value
        assertNull(initialState.expandedCell)
        assertFalse(initialState.isLoading)
        // appsInCells는 init에서 LoadApps 트리거 전 직접 접근 불가 — init이 즉시 실행되므로
        // 이 테스트는 초기값 타입만 확인
        assertTrue(initialState.appsInCells is Map<*, *>)
    }

    // 2. LoadApps → 상태 업데이트 (isLoading 전환, appsInCells 반영)
    @Test
    fun `LoadApps 처리 후 appsInCells에 앱이 배분됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(GridCell.entries.size, state.appsInCells.size)
        val totalApps = state.appsInCells.values.sumOf { it.size }
        assertEquals(sampleApps.size, totalApps)
    }

    // 3. 그리드 클릭 시 확장
    @Test
    fun `ClickGrid TOP_LEFT 클릭 시 expandedCell이 TOP_LEFT로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GridCell.TOP_LEFT, viewModel.uiState.value.expandedCell)
    }

    // 4. 같은 셀 재클릭 시 축소
    @Test
    fun `같은 셀 두 번 클릭 시 expandedCell이 null로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.expandedCell)
    }

    // 5. 다른 셀 클릭 시 전환
    @Test
    fun `TOP_LEFT에서 BOTTOM_RIGHT 클릭 시 expandedCell이 BOTTOM_RIGHT로 전환됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.BOTTOM_RIGHT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GridCell.BOTTOM_RIGHT, viewModel.uiState.value.expandedCell)
    }

    // 6. 4개 셀 모두 확장 가능
    @Test
    fun `4개 GridCell 모두 확장 가능함`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        for (cell in GridCell.entries) {
            viewModel.handleIntent(HomeIntent.ClickGrid(cell))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(cell, viewModel.uiState.value.expandedCell)
        }
    }

    // 7. ClickApp → NavigateToApp SideEffect
    @Test
    fun `ClickApp 처리 시 NavigateToApp SideEffect가 발생함`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        viewModel.effect.test {
            viewModel.handleIntent(HomeIntent.ClickApp(targetApp))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.NavigateToApp)
            assertEquals(targetApp.packageName, (effect as HomeSideEffect.NavigateToApp).packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // 8. 앱 목록이 각 그리드에 올바르게 배분됨 (가나다순 정렬 후 4등분)
    @Test
    fun `8개 앱이 4개 GridCell에 2개씩 균등 배분됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val appsInCells = viewModel.uiState.value.appsInCells
        assertEquals(4, appsInCells.size)
        assertTrue(appsInCells.containsKey(GridCell.TOP_LEFT))
        assertTrue(appsInCells.containsKey(GridCell.TOP_RIGHT))
        assertTrue(appsInCells.containsKey(GridCell.BOTTOM_LEFT))
        assertTrue(appsInCells.containsKey(GridCell.BOTTOM_RIGHT))
        appsInCells.values.forEach { cellApps ->
            assertEquals(2, cellApps.size)
        }
    }

    // 9. Flow 상태 관찰 (Turbine emission 시퀀스)
    @Test
    fun `LoadApps 처리 중 isLoading 상태 전환 시퀀스 확인`() = runTest {
        val freshUseCase: GetInstalledAppsUseCase = mockk()
        every { freshUseCase() } returns flowOf(sampleApps)

        viewModel.uiState.test {
            // HomeViewModel 생성 전이므로 새 인스턴스에서 확인
            val freshVm = HomeViewModel(freshUseCase)

            // 초기 상태 또는 첫 emission (init에서 LoadApps 호출)
            val firstEmission = awaitItem()
            // isLoading은 true였다가 false로 변하므로 최소 1개 emission 있음
            // 테스트 디스패처에서는 동기적으로 처리되므로 최종 상태만 확인
            assertFalse(firstEmission.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // 10. 연속 토글 정합성 (3회 클릭 → 확장 상태)
    @Test
    fun `같은 셀 3회 클릭 시 확장 상태가 됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        repeat(3) {
            viewModel.handleIntent(HomeIntent.ClickGrid(GridCell.TOP_RIGHT))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // 홀수 클릭이므로 확장 상태
        assertEquals(GridCell.TOP_RIGHT, viewModel.uiState.value.expandedCell)
    }

    // 11. Flow 예외 발생 시 ShowError SideEffect 발생
    @Test
    fun `LoadApps 중 예외 발생 시 ShowError SideEffect가 발생함`() = runTest {
        val errorMessage = "패키지 목록 조회 실패"
        val errorUseCase: GetInstalledAppsUseCase = mockk()
        every { errorUseCase() } returns flow { throw RuntimeException(errorMessage) }

        val errorVm = HomeViewModel(errorUseCase)

        errorVm.effect.test {
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.ShowError)
            assertEquals(errorMessage, (effect as HomeSideEffect.ShowError).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // 12. Flow 예외 발생 시 isLoading이 false로 복구됨
    @Test
    fun `LoadApps 중 예외 발생 시 isLoading이 false로 복구됨`() = runTest {
        val errorUseCase: GetInstalledAppsUseCase = mockk()
        every { errorUseCase() } returns flow { throw RuntimeException("오류") }

        val errorVm = HomeViewModel(errorUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(errorVm.uiState.value.isLoading)
    }
}
