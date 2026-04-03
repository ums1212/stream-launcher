package org.comon.streamlauncher.settings.livewallpaper

import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.usecase.DeleteLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.GetAllLiveWallpapersUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SetLiveWallpaperUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveWallpaperSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var getAllLiveWallpapersUseCase: GetAllLiveWallpapersUseCase
    private lateinit var saveLiveWallpaperUseCase: SaveLiveWallpaperUseCase
    private lateinit var deleteLiveWallpaperUseCase: DeleteLiveWallpaperUseCase
    private lateinit var setLiveWallpaperUseCase: SetLiveWallpaperUseCase
    private lateinit var wallpaperHelper: WallpaperHelper
    private lateinit var viewModel: LiveWallpaperSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        getAllLiveWallpapersUseCase = mockk()
        every { getAllLiveWallpapersUseCase() } returns flowOf(emptyList())
        saveLiveWallpaperUseCase = mockk(relaxed = true)
        deleteLiveWallpaperUseCase = mockk(relaxed = true)
        setLiveWallpaperUseCase = mockk(relaxed = true)
        wallpaperHelper = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        allLiveWallpapers: GetAllLiveWallpapersUseCase = getAllLiveWallpapersUseCase,
        saveLiveWallpaper: SaveLiveWallpaperUseCase = saveLiveWallpaperUseCase,
        deleteLiveWallpaper: DeleteLiveWallpaperUseCase = deleteLiveWallpaperUseCase,
        setLiveWallpaper: SetLiveWallpaperUseCase = setLiveWallpaperUseCase,
        wallpaper: WallpaperHelper = wallpaperHelper,
    ) = LiveWallpaperSettingsViewModel(settingsUseCase, allLiveWallpapers, saveLiveWallpaper, deleteLiveWallpaper, setLiveWallpaper, wallpaper)

    @Test
    fun `SwitchOrientationTab LANDSCAPE 처리 시 selectedOrientationTab이 LANDSCAPE로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.PORTRAIT, viewModel.uiState.value.selectedOrientationTab)

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.LANDSCAPE, viewModel.uiState.value.selectedOrientationTab)
    }

    @Test
    fun `SwitchOrientationTab PORTRAIT 처리 시 selectedOrientationTab이 PORTRAIT로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(LiveWallpaperSettingsIntent.SwitchOrientationTab(WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WallpaperOrientation.PORTRAIT, viewModel.uiState.value.selectedOrientationTab)
    }

    @Test
    fun `SetActiveLiveWallpaper PORTRAIT 처리 시 setLiveWallpaperUseCase가 PORTRAIT로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SetActiveLiveWallpaper(1, "/path/a.mp4", WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(1, "/path/a.mp4", WallpaperOrientation.PORTRAIT) }
    }

    @Test
    fun `SetActiveLiveWallpaper LANDSCAPE 처리 시 setLiveWallpaperUseCase가 LANDSCAPE로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SetActiveLiveWallpaper(2, "/path/b.mp4", WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(2, "/path/b.mp4", WallpaperOrientation.LANDSCAPE) }
    }

    @Test
    fun `ClearActiveLiveWallpaper PORTRAIT 처리 시 setLiveWallpaperUseCase가 null로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.ClearActiveLiveWallpaper(WallpaperOrientation.PORTRAIT))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(null, null, WallpaperOrientation.PORTRAIT) }
    }

    @Test
    fun `ClearActiveLiveWallpaper LANDSCAPE 처리 시 setLiveWallpaperUseCase가 LANDSCAPE null로 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.ClearActiveLiveWallpaper(WallpaperOrientation.LANDSCAPE))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setLiveWallpaperUseCase(null, null, WallpaperOrientation.LANDSCAPE) }
    }

    @Test
    fun `LoadLiveWallpaperFile - portrait 탭에서 selectedLiveWallpaperUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SwitchOrientationTab(WallpaperOrientation.PORTRAIT))
        viewModel.handleIntent(LiveWallpaperSettingsIntent.LoadLiveWallpaperFile("content://media/1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://media/1", viewModel.uiState.value.selectedLiveWallpaperUri)
        assertNull(viewModel.uiState.value.selectedLiveWallpaperLandscapeUri)
    }

    @Test
    fun `LoadLiveWallpaperFile - landscape 탭에서 selectedLiveWallpaperLandscapeUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(LiveWallpaperSettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
        viewModel.handleIntent(LiveWallpaperSettingsIntent.LoadLiveWallpaperFile("content://media/2"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://media/2", viewModel.uiState.value.selectedLiveWallpaperLandscapeUri)
        assertNull(viewModel.uiState.value.selectedLiveWallpaperUri)
    }

    @Test
    fun `SetActiveLiveWallpaper 성공 시 LaunchLiveWallpaperPicker SideEffect가 발행됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleIntent(LiveWallpaperSettingsIntent.SetActiveLiveWallpaper(1, "/path/a.mp4", WallpaperOrientation.PORTRAIT))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LiveWallpaperSettingsSideEffect.LaunchLiveWallpaperPicker, awaitItem())
        }
    }
}
