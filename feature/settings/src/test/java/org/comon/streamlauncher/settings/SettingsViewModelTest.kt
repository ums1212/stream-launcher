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
import org.comon.streamlauncher.domain.model.WallpaperOrientation
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
import org.comon.streamlauncher.domain.usecase.DeleteLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.GetAllLiveWallpapersUseCase
import org.comon.streamlauncher.domain.usecase.SaveLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SetLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.UpdateMarketPresetIdUseCase
import org.comon.streamlauncher.domain.usecase.ObserveAuthStateUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.settings.upload.UploadDataHolder
import org.comon.streamlauncher.settings.upload.UploadProgressTracker
import org.comon.streamlauncher.settings.model.ImageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase
    private lateinit var observeAuthStateUseCase: ObserveAuthStateUseCase
    private lateinit var uploadProgressTracker: UploadProgressTracker
    private lateinit var uploadDataHolder: UploadDataHolder
    private lateinit var updateMarketPresetIdUseCase: UpdateMarketPresetIdUseCase
    private lateinit var getAllLiveWallpapersUseCase: GetAllLiveWallpapersUseCase
    private lateinit var saveLiveWallpaperUseCase: SaveLiveWallpaperUseCase
    private lateinit var deleteLiveWallpaperUseCase: DeleteLiveWallpaperUseCase
    private lateinit var setLiveWallpaperUseCase: SetLiveWallpaperUseCase
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
        signInWithGoogleUseCase = mockk(relaxed = true)
        observeAuthStateUseCase = mockk()
        every { observeAuthStateUseCase() } returns flowOf(null)

        uploadProgressTracker = UploadProgressTracker()
        uploadDataHolder = UploadDataHolder()
        updateMarketPresetIdUseCase = mockk(relaxed = true)

        getAllLiveWallpapersUseCase = mockk()
        every { getAllLiveWallpapersUseCase() } returns flowOf(emptyList())
        saveLiveWallpaperUseCase = mockk(relaxed = true)
        deleteLiveWallpaperUseCase = mockk(relaxed = true)
        setLiveWallpaperUseCase = mockk(relaxed = true)

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
        signInWithGoogle: SignInWithGoogleUseCase = signInWithGoogleUseCase,
        observeAuthState: ObserveAuthStateUseCase = observeAuthStateUseCase,
        progressTracker: UploadProgressTracker = uploadProgressTracker,
        dataHolder: UploadDataHolder = uploadDataHolder,
        updateMarketPresetId: UpdateMarketPresetIdUseCase = updateMarketPresetIdUseCase,
        getAllLiveWallpapers: GetAllLiveWallpapersUseCase = getAllLiveWallpapersUseCase,
        saveLiveWallpaper: SaveLiveWallpaperUseCase = saveLiveWallpaperUseCase,
        deleteLiveWallpaper: DeleteLiveWallpaperUseCase = deleteLiveWallpaperUseCase,
        setLiveWallpaper: SetLiveWallpaperUseCase = setLiveWallpaperUseCase,
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
        signInWithGoogle,
        observeAuthState,
        progressTracker,
        dataHolder,
        updateMarketPresetId,
        getAllLiveWallpapers,
        saveLiveWallpaper,
        deleteLiveWallpaper,
        setLiveWallpaper,
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

    // 14. SwitchOrientationTab → selectedOrientationTab 상태 업데이트
    @Test
    fun `SwitchOrientationTab LANDSCAPE 처리 시 selectedOrientationTab이 LANDSCAPE로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.PORTRAIT, viewModel.uiState.value.selectedOrientationTab)

        viewModel.handleIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.LANDSCAPE, viewModel.uiState.value.selectedOrientationTab)
    }

    // 15. SwitchOrientationTab PORTRAIT → 다시 PORTRAIT로 복귀
    @Test
    fun `SwitchOrientationTab PORTRAIT 처리 시 selectedOrientationTab이 PORTRAIT로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.PORTRAIT, viewModel.uiState.value.selectedOrientationTab)
    }

    // 16. SetActiveLiveWallpaper PORTRAIT → setLiveWallpaperUseCase PORTRAIT로 호출
    @Test
    fun `SetActiveLiveWallpaper PORTRAIT 처리 시 setLiveWallpaperUseCase가 PORTRAIT로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SetActiveLiveWallpaper(1, "/path/a.mp4", WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(1, "/path/a.mp4", WallpaperOrientation.PORTRAIT) }
    }

    // 17. SetActiveLiveWallpaper LANDSCAPE → setLiveWallpaperUseCase LANDSCAPE로 호출
    @Test
    fun `SetActiveLiveWallpaper LANDSCAPE 처리 시 setLiveWallpaperUseCase가 LANDSCAPE로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SetActiveLiveWallpaper(2, "/path/b.mp4", WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(2, "/path/b.mp4", WallpaperOrientation.LANDSCAPE) }
    }

    // 18. ClearActiveLiveWallpaper PORTRAIT → setLiveWallpaperUseCase(null, null, PORTRAIT) 호출
    @Test
    fun `ClearActiveLiveWallpaper PORTRAIT 처리 시 setLiveWallpaperUseCase가 null로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.ClearActiveLiveWallpaper(WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(null, null, WallpaperOrientation.PORTRAIT) }
    }

    // 19. ClearActiveLiveWallpaper LANDSCAPE → setLiveWallpaperUseCase(null, null, LANDSCAPE) 호출
    @Test
    fun `ClearActiveLiveWallpaper LANDSCAPE 처리 시 setLiveWallpaperUseCase가 LANDSCAPE null로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.ClearActiveLiveWallpaper(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(null, null, WallpaperOrientation.LANDSCAPE) }
    }

    // 20. LoadLiveWallpaperFile - portrait 탭에서 portrait URI 업데이트
    @Test
    fun `LoadLiveWallpaperFile - portrait 탭에서 selectedLiveWallpaperUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.PORTRAIT))
        viewModel.handleIntent(SettingsIntent.LoadLiveWallpaperFile("content://media/1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://media/1", viewModel.uiState.value.selectedLiveWallpaperUri)
        assertNull(viewModel.uiState.value.selectedLiveWallpaperLandscapeUri)
    }

    // 21. LoadLiveWallpaperFile - landscape 탭에서 landscape URI 업데이트
    @Test
    fun `LoadLiveWallpaperFile - landscape 탭에서 selectedLiveWallpaperLandscapeUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        viewModel.handleIntent(SettingsIntent.LoadLiveWallpaperFile("content://media/2"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://media/2", viewModel.uiState.value.selectedLiveWallpaperLandscapeUri)
        assertNull(viewModel.uiState.value.selectedLiveWallpaperUri)
    }
}
