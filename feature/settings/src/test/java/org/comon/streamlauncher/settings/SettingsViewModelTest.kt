package org.comon.streamlauncher.settings

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DeletePresetUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetAllPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.domain.usecase.SavePresetUseCase
import org.comon.streamlauncher.settings.model.ImageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveColorPresetUseCase: SaveColorPresetUseCase
    private lateinit var saveGridCellImageUseCase: SaveGridCellImageUseCase
    private lateinit var saveFeedSettingsUseCase: SaveFeedSettingsUseCase
    private lateinit var saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase
    private lateinit var checkNoticeUseCase: CheckNoticeUseCase
    private lateinit var dismissNoticeUseCase: DismissNoticeUseCase
    private lateinit var getAllPresetsUseCase: GetAllPresetsUseCase
    private lateinit var savePresetUseCase: SavePresetUseCase
    private lateinit var deletePresetUseCase: DeletePresetUseCase
    private lateinit var wallpaperHelper: org.comon.streamlauncher.domain.util.WallpaperHelper
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())

        saveColorPresetUseCase = mockk(relaxed = true)
        saveGridCellImageUseCase = mockk(relaxed = true)
        saveFeedSettingsUseCase = mockk(relaxed = true)
        saveAppDrawerSettingsUseCase = mockk(relaxed = true)
        checkNoticeUseCase = mockk(relaxed = true)
        dismissNoticeUseCase = mockk(relaxed = true)

        getAllPresetsUseCase = mockk()
        every { getAllPresetsUseCase() } returns flowOf(emptyList())
        savePresetUseCase = mockk(relaxed = true)
        deletePresetUseCase = mockk(relaxed = true)
        wallpaperHelper = mockk(relaxed = true)

        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        colorSaveUseCase: SaveColorPresetUseCase = saveColorPresetUseCase,
        imageSaveUseCase: SaveGridCellImageUseCase = saveGridCellImageUseCase,
        feedSettingsUseCase: SaveFeedSettingsUseCase = saveFeedSettingsUseCase,
        appDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase = saveAppDrawerSettingsUseCase,
        checkNotice: CheckNoticeUseCase = checkNoticeUseCase,
        dismissNotice: DismissNoticeUseCase = dismissNoticeUseCase,
        getAllPresets: GetAllPresetsUseCase = getAllPresetsUseCase,
        savePreset: SavePresetUseCase = savePresetUseCase,
        deletePreset: DeletePresetUseCase = deletePresetUseCase,
        wallpaperUtil: org.comon.streamlauncher.domain.util.WallpaperHelper = wallpaperHelper,
    ): SettingsViewModel = SettingsViewModel(
        settingsUseCase,
        colorSaveUseCase,
        imageSaveUseCase,
        feedSettingsUseCase,
        appDrawerSettingsUseCase,
        checkNotice,
        dismissNotice,
        getAllPresets,
        savePreset,
        deletePreset,
        wallpaperUtil,
    )

    // 1. ChangeAccentColor → colorPresetIndex 상태 업데이트
    @Test
    fun `ChangeAccentColor 처리 시 colorPresetIndex 상태가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.ChangeAccentColor(3))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.colorPresetIndex)
    }

    // 2. ChangeAccentColor → saveColorPresetUseCase 호출
    @Test
    fun `ChangeAccentColor 처리 시 saveColorPresetUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.ChangeAccentColor(5))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveColorPresetUseCase(5) }
    }

    // 3. SetGridImage IDLE → gridCellImages 상태 업데이트
    @Test
    fun `SetGridImage IDLE 처리 시 해당 셀의 idleImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/1"
        viewModel.handleIntent(SettingsIntent.SetGridImage(GridCell.TOP_LEFT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.TOP_LEFT]
        assertEquals(testUri, cellImage?.idleImageUri)
    }

    // 4. SetGridImage EXPANDED → gridCellImages 상태 업데이트
    @Test
    fun `SetGridImage EXPANDED 처리 시 해당 셀의 expandedImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/2"
        viewModel.handleIntent(SettingsIntent.SetGridImage(GridCell.BOTTOM_RIGHT, ImageType.EXPANDED, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.BOTTOM_RIGHT]
        assertEquals(testUri, cellImage?.expandedImageUri)
    }

    // 5. SetGridImage → saveGridCellImageUseCase 호출
    @Test
    fun `SetGridImage 처리 시 saveGridCellImageUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/3"
        viewModel.handleIntent(SettingsIntent.SetGridImage(GridCell.TOP_RIGHT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveGridCellImageUseCase(GridCell.TOP_RIGHT, testUri, null) }
    }

    // 6. 초기 설정 로드 → colorPresetIndex 복원
    @Test
    fun `초기 설정 로드 시 colorPresetIndex가 저장된 값으로 복원됨`() = runTest {
        val savedSettings = LauncherSettings(colorPresetIndex = 4)
        val settingsUseCase: GetLauncherSettingsUseCase = mockk()
        every { settingsUseCase() } returns flowOf(savedSettings)

        val restoredVm = makeViewModel(settingsUseCase = settingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, restoredVm.uiState.value.colorPresetIndex)
    }

    // 7. SaveAppDrawerSettings → state 업데이트 및 유스케이스 호출
    @Test
    fun `SaveAppDrawerSettings 처리 시 설정 상태가 업데이트되고 유스케이스가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SaveAppDrawerSettings(5, 7, 1.2f))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.appDrawerGridColumns)
        assertEquals(7, state.appDrawerGridRows)
        assertEquals(1.2f, state.appDrawerIconSizeRatio, 0.0f)

        coVerify { saveAppDrawerSettingsUseCase(5, 7, 1.2f) }
    }

    // 8. SaveFeedSettings → state 업데이트 및 유스케이스 호출
    @Test
    fun `SaveFeedSettings 처리 시 설정 상태가 업데이트되고 유스케이스가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(
            SettingsIntent.SaveFeedSettings(
                chzzkChannelId = "chzzk123",
                youtubeChannelId = "yt456"
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("chzzk123", state.chzzkChannelId)
        assertEquals("yt456", state.youtubeChannelId)

        coVerify { saveFeedSettingsUseCase("chzzk123", "yt456") }
    }

    // 9. ResetTab → NavigateToMain side effect 발행
    @Test
    fun `ResetTab 처리 시 NavigateToMain side effect가 발행됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleIntent(SettingsIntent.ResetTab)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SettingsSideEffect.NavigateToMain, awaitItem())
        }
    }

    // 10. ShowNotice → showNoticeDialog = true
    @Test
    fun `ShowNotice 처리 시 showNoticeDialog가 true로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoticeDialog)

        viewModel.handleIntent(SettingsIntent.ShowNotice)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showNoticeDialog)
    }

    // 11. DismissNotice → showNoticeDialog = false
    @Test
    fun `DismissNotice 처리 시 showNoticeDialog가 false로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.ShowNotice)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showNoticeDialog)

        viewModel.handleIntent(SettingsIntent.DismissNotice)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoticeDialog)
    }

    // 12. checkNotice → checkNoticeUseCase가 true일 때 showNoticeDialog = true
    @Test
    fun `checkNotice 호출 시 checkNoticeUseCase가 true이면 showNoticeDialog가 true로 변경됨`() = runTest {
        coEvery { checkNoticeUseCase(any()) } returns true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkNotice("1.0.0")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showNoticeDialog)
    }

    // 13. checkNotice → checkNoticeUseCase가 false일 때 showNoticeDialog = false 유지
    @Test
    fun `checkNotice 호출 시 checkNoticeUseCase가 false이면 showNoticeDialog가 false로 유지됨`() = runTest {
        coEvery { checkNoticeUseCase(any()) } returns false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkNotice("1.0.0")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoticeDialog)
    }
}
