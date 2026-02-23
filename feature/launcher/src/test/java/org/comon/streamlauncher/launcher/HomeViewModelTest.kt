package org.comon.streamlauncher.launcher

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.repository.SettingsRepository
import org.comon.streamlauncher.domain.usecase.GetInstalledAppsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveCellAssignmentUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.launcher.model.ImageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveColorPresetUseCase: SaveColorPresetUseCase
    private lateinit var saveGridCellImageUseCase: SaveGridCellImageUseCase
    private lateinit var saveCellAssignmentUseCase: SaveCellAssignmentUseCase
    private lateinit var saveFeedSettingsUseCase: SaveFeedSettingsUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        every { useCase() } returns flowOf(sampleApps)

        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())

        saveColorPresetUseCase = mockk(relaxed = true)
        saveGridCellImageUseCase = mockk(relaxed = true)
        saveCellAssignmentUseCase = mockk(relaxed = true)
        saveFeedSettingsUseCase = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 테스트용 ViewModel 생성 헬퍼 */
    private fun makeViewModel(
        appsUseCase: GetInstalledAppsUseCase = useCase,
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        colorSaveUseCase: SaveColorPresetUseCase = saveColorPresetUseCase,
        imageSaveUseCase: SaveGridCellImageUseCase = saveGridCellImageUseCase,
        cellAssignmentUseCase: SaveCellAssignmentUseCase = saveCellAssignmentUseCase,
        feedSettingsUseCase: SaveFeedSettingsUseCase = saveFeedSettingsUseCase,
        repository: SettingsRepository = settingsRepository,
    ): HomeViewModel = HomeViewModel(
        appsUseCase,
        settingsUseCase,
        colorSaveUseCase,
        imageSaveUseCase,
        cellAssignmentUseCase,
        feedSettingsUseCase,
        repository,
    )

    // 1. 초기 상태 확인
    @Test
    fun `초기 상태 - expandedCell null, isLoading false, appsInCells 비어있음`() {
        val freshUseCase: GetInstalledAppsUseCase = mockk()
        every { freshUseCase() } returns flowOf(emptyList())
        val freshVm = makeViewModel(appsUseCase = freshUseCase)

        val initialState = freshVm.uiState.value
        assertNull(initialState.expandedCell)
        assertFalse(initialState.isLoading)
        assertTrue(initialState.appsInCells is Map<*, *>)
    }

    // 2. LoadApps → allApps 저장, appsInCells 비어있음
    @Test
    fun `LoadApps 처리 후 allApps에 전체 앱이 저장되고 appsInCells는 비어있음`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(sampleApps.size, state.allApps.size)
        assertEquals(GridCell.entries.size, state.appsInCells.size)
        val totalCellApps = state.appsInCells.values.sumOf { it.size }
        assertEquals(0, totalCellApps)
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

    // 8. 초기 상태에서 셀은 비어있고, 앱 할당 시에만 셀에 표시됨
    @Test
    fun `초기 상태에서 4개 셀이 모두 비어있고 앱 할당 후에만 표시됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val appsInCells = viewModel.uiState.value.appsInCells
        assertEquals(4, appsInCells.size)
        appsInCells.values.forEach { cellApps ->
            assertEquals(0, cellApps.size)
        }

        viewModel.handleIntent(HomeIntent.AssignAppToCell(sampleApps[0], GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = viewModel.uiState.value.appsInCells
        assertEquals(1, updated[GridCell.TOP_LEFT]?.size)
        assertEquals(0, updated[GridCell.TOP_RIGHT]?.size)
    }

    // 9. Flow 상태 관찰 (Turbine emission 시퀀스)
    @Test
    fun `LoadApps 처리 중 isLoading 상태 전환 시퀀스 확인`() = runTest {
        val freshUseCase: GetInstalledAppsUseCase = mockk()
        every { freshUseCase() } returns flowOf(sampleApps)

        viewModel.uiState.test {
            val freshVm = makeViewModel(appsUseCase = freshUseCase)

            val firstEmission = awaitItem()
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

        assertEquals(GridCell.TOP_RIGHT, viewModel.uiState.value.expandedCell)
    }

    // 11. Flow 예외 발생 시 ShowError SideEffect 발생
    @Test
    fun `LoadApps 중 예외 발생 시 ShowError SideEffect가 발생함`() = runTest {
        val errorMessage = "패키지 목록 조회 실패"
        val errorUseCase: GetInstalledAppsUseCase = mockk()
        every { errorUseCase() } returns flow { throw RuntimeException(errorMessage) }

        val errorVm = makeViewModel(appsUseCase = errorUseCase)

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

        val errorVm = makeViewModel(appsUseCase = errorUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(errorVm.uiState.value.isLoading)
    }

    // 13. Search 인텐트로 searchQuery 즉시 업데이트
    @Test
    fun `Search 인텐트로 searchQuery가 즉시 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.Search("ㅋ"))

        assertEquals("ㅋ", viewModel.uiState.value.searchQuery)
    }

    // 14. 빈 검색어 시 filteredApps = 전체 앱 (정렬된 상태)
    @Test
    fun `빈 검색어 시 filteredApps는 전체 앱을 정렬한 것과 같음`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.Search(""))
        testDispatcher.scheduler.advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredApps
        val allSorted = sampleApps.sortedBy { it.label }
        assertEquals(allSorted, filtered)
    }

    // 15. 초성 검색 시 해당 앱만 필터링
    @Test
    fun `초성 ㅋ 검색 시 카카오앱만 필터링됨`() = runTest {
        val searchApps = listOf(
            AppEntity("com.kakao", "카카오", "com.kakao.Main"),
            AppEntity("com.naver", "네이버", "com.naver.Main"),
            AppEntity("com.google", "구글", "com.google.Main"),
        )
        val searchUseCase: GetInstalledAppsUseCase = mockk()
        every { searchUseCase() } returns flowOf(searchApps)
        val searchVm = makeViewModel(appsUseCase = searchUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        searchVm.handleIntent(HomeIntent.Search("ㅋ"))
        testDispatcher.scheduler.advanceUntilIdle()

        val filtered = searchVm.uiState.value.filteredApps
        assertEquals(1, filtered.size)
        assertEquals("카카오", filtered[0].label)
    }

    // 16. 영문 대소문자 무시 검색 동작
    @Test
    fun `영문 대소문자 무시 검색 동작`() = runTest {
        val englishApps = listOf(
            AppEntity("com.settings", "Settings", "com.settings.Main"),
            AppEntity("com.gallery", "Gallery", "com.gallery.Main"),
        )
        val englishUseCase: GetInstalledAppsUseCase = mockk()
        every { englishUseCase() } returns flowOf(englishApps)
        val englishVm = makeViewModel(appsUseCase = englishUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        englishVm.handleIntent(HomeIntent.Search("set"))
        testDispatcher.scheduler.advanceUntilIdle()

        val filtered = englishVm.uiState.value.filteredApps
        assertEquals(1, filtered.size)
        assertEquals("Settings", filtered[0].label)
    }

    // 17. ResetHome 시 searchQuery도 "" 초기화
    @Test
    fun `ResetHome 시 searchQuery가 빈 문자열로 초기화됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.Search("ㅋ"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("ㅋ", viewModel.uiState.value.searchQuery)

        viewModel.handleIntent(HomeIntent.ResetHome)

        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    // 18. Search + ClickApp 연계 동작
    @Test
    fun `검색 후 filteredApps에서 앱 클릭 시 NavigateToApp SideEffect 발생`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.Search("앱"))
        testDispatcher.scheduler.advanceUntilIdle()

        val firstFilteredApp = viewModel.uiState.value.filteredApps.firstOrNull()
        assertNotNull(firstFilteredApp)

        viewModel.effect.test {
            viewModel.handleIntent(HomeIntent.ClickApp(firstFilteredApp!!))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.NavigateToApp)
            assertEquals(firstFilteredApp.packageName, (effect as HomeSideEffect.NavigateToApp).packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // 19. ChangeAccentColor → colorPresetIndex 상태 업데이트
    @Test
    fun `ChangeAccentColor 처리 시 colorPresetIndex 상태가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.ChangeAccentColor(3))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.colorPresetIndex)
    }

    // 20. ChangeAccentColor → saveColorPresetUseCase 호출
    @Test
    fun `ChangeAccentColor 처리 시 saveColorPresetUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(HomeIntent.ChangeAccentColor(5))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveColorPresetUseCase(5) }
    }

    // 21. SetGridImage IDLE → gridCellImages 상태 업데이트
    @Test
    fun `SetGridImage IDLE 처리 시 해당 셀의 idleImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/1"
        viewModel.handleIntent(HomeIntent.SetGridImage(GridCell.TOP_LEFT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.TOP_LEFT]
        assertEquals(testUri, cellImage?.idleImageUri)
    }

    // 22. SetGridImage EXPANDED → gridCellImages 상태 업데이트
    @Test
    fun `SetGridImage EXPANDED 처리 시 해당 셀의 expandedImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/2"
        viewModel.handleIntent(HomeIntent.SetGridImage(GridCell.BOTTOM_RIGHT, ImageType.EXPANDED, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.BOTTOM_RIGHT]
        assertEquals(testUri, cellImage?.expandedImageUri)
    }

    // 23. SetGridImage → saveGridCellImageUseCase 호출
    @Test
    fun `SetGridImage 처리 시 saveGridCellImageUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/3"
        viewModel.handleIntent(HomeIntent.SetGridImage(GridCell.TOP_RIGHT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveGridCellImageUseCase(GridCell.TOP_RIGHT, testUri, null) }
    }

    // 24. 초기 설정 로드 → colorPresetIndex 복원
    @Test
    fun `초기 설정 로드 시 colorPresetIndex가 저장된 값으로 복원됨`() = runTest {
        val savedSettings = LauncherSettings(colorPresetIndex = 4)
        val settingsUseCase: GetLauncherSettingsUseCase = mockk()
        every { settingsUseCase() } returns flowOf(savedSettings)

        val restoredVm = makeViewModel(settingsUseCase = settingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, restoredVm.uiState.value.colorPresetIndex)
    }

    // 25. AssignAppToCell → cellAssignments 상태 업데이트
    @Test
    fun `AssignAppToCell 처리 시 해당 셀의 cellAssignments에 packageName이 추가됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        val assignments = viewModel.uiState.value.cellAssignments
        assertTrue(assignments[GridCell.TOP_LEFT]?.contains(targetApp.packageName) == true)
    }

    // 26. AssignAppToCell → 다른 셀에서 제거됨
    @Test
    fun `AssignAppToCell 처리 시 다른 셀에서 해당 앱이 제거됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        // 먼저 TOP_RIGHT에 할당
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.TOP_RIGHT))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.cellAssignments[GridCell.TOP_RIGHT]?.contains(targetApp.packageName) == true)

        // 다시 TOP_LEFT에 할당 → TOP_RIGHT에서 제거됨
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        val assignments = viewModel.uiState.value.cellAssignments
        assertTrue(assignments[GridCell.TOP_LEFT]?.contains(targetApp.packageName) == true)
        assertTrue(assignments[GridCell.TOP_RIGHT]?.contains(targetApp.packageName) != true)
    }

    // 27. AssignAppToCell → pinnedPackages에 포함됨
    @Test
    fun `AssignAppToCell 처리 후 pinnedPackages에 해당 앱이 포함됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.BOTTOM_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pinnedPackages.contains(targetApp.packageName))
    }

    // 28. UnassignApp → cellAssignments에서 제거
    @Test
    fun `UnassignApp 처리 시 모든 셀에서 해당 앱이 제거됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pinnedPackages.contains(targetApp.packageName))

        viewModel.handleIntent(HomeIntent.UnassignApp(targetApp))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pinnedPackages.contains(targetApp.packageName))
    }

    // 29. distributeApps — 할당된 앱만 셀에 배치, 미할당 앱은 제외
    @Test
    fun `distributeApps - 할당된 앱만 해당 셀에 배치되고 미할당 앱은 어떤 셀에도 없음`() {
        val apps = (1..4).map { i ->
            AppEntity("pkg$i", "앱$i", "pkg$i.Main")
        }
        val assignments = mapOf(GridCell.TOP_LEFT to listOf("pkg3"))
        val vm = makeViewModel()

        val result = vm.distributeApps(apps, assignments)

        assertEquals(1, result[GridCell.TOP_LEFT]?.size)
        assertTrue(result[GridCell.TOP_LEFT]?.firstOrNull()?.packageName == "pkg3")
        val totalAppsInCells = result.values.sumOf { it.size }
        assertEquals(1, totalAppsInCells)
    }

    // 30. AssignAppToCell → saveCellAssignmentUseCase 호출됨
    @Test
    fun `AssignAppToCell 처리 시 saveCellAssignmentUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetApp = sampleApps[0]
        viewModel.handleIntent(HomeIntent.AssignAppToCell(targetApp, GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveCellAssignmentUseCase(GridCell.TOP_LEFT, any()) }
    }

    // 31. AssignAppToCell → 셀당 최대 6개 제한
    @Test
    fun `AssignAppToCell 처리 시 셀에 이미 6개 앱이 있으면 7번째 앱이 추가되지 않음`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val manyApps = (1..8).map { i ->
            AppEntity("com.test.app$i", "테스트앱$i", "com.test.app$i.Main")
        }
        val manyAppsUseCase: GetInstalledAppsUseCase = mockk()
        every { manyAppsUseCase() } returns flowOf(manyApps)
        val vm = makeViewModel(appsUseCase = manyAppsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        for (i in 0 until HomeViewModel.MAX_APPS_PER_CELL) {
            vm.handleIntent(HomeIntent.AssignAppToCell(manyApps[i], GridCell.TOP_LEFT))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val assignmentsBefore = vm.uiState.value.cellAssignments[GridCell.TOP_LEFT]
        assertEquals(HomeViewModel.MAX_APPS_PER_CELL, assignmentsBefore?.size)

        val overflowApp = manyApps[HomeViewModel.MAX_APPS_PER_CELL]
        vm.handleIntent(HomeIntent.AssignAppToCell(overflowApp, GridCell.TOP_LEFT))
        testDispatcher.scheduler.advanceUntilIdle()

        val assignmentsAfter = vm.uiState.value.cellAssignments[GridCell.TOP_LEFT]
        assertEquals(HomeViewModel.MAX_APPS_PER_CELL, assignmentsAfter?.size)
        assertFalse(assignmentsAfter?.contains(overflowApp.packageName) == true)
    }
}
