package org.comon.streamlauncher.settings

import app.cash.turbine.test
import io.mockk.coEvery
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
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SetStaticWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
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
    private lateinit var checkNoticeUseCase: CheckNoticeUseCase
    private lateinit var dismissNoticeUseCase: DismissNoticeUseCase
    private lateinit var setStaticWallpaperUseCase: SetStaticWallpaperUseCase
    private lateinit var wallpaperHelper: WallpaperHelper
    private lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        checkNoticeUseCase = mockk(relaxed = true)
        dismissNoticeUseCase = mockk(relaxed = true)
        setStaticWallpaperUseCase = mockk(relaxed = true)
        wallpaperHelper = mockk(relaxed = true)
        signInWithGoogleUseCase = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        checkNotice: CheckNoticeUseCase = checkNoticeUseCase,
        dismissNotice: DismissNoticeUseCase = dismissNoticeUseCase,
        setStaticWallpaper: SetStaticWallpaperUseCase = setStaticWallpaperUseCase,
        wallpaper: WallpaperHelper = wallpaperHelper,
        signInWithGoogle: SignInWithGoogleUseCase = signInWithGoogleUseCase,
    ) = SettingsViewModel(settingsUseCase, checkNotice, dismissNotice, setStaticWallpaper, wallpaper, signInWithGoogle)

    @Test
    fun `ResetTab 처리 시 NavigateToMain side effect가 발행됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleIntent(SettingsIntent.ResetTab)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SettingsSideEffect.NavigateToMain, awaitItem())
        }
    }

    @Test
    fun `ShowNotice 처리 시 showNoticeDialog가 true로 변경됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoticeDialog)

        viewModel.handleIntent(SettingsIntent.ShowNotice)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showNoticeDialog)
    }

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

    @Test
    fun `checkNotice 호출 시 checkNoticeUseCase가 true이면 showNoticeDialog가 true로 변경됨`() = runTest {
        coEvery { checkNoticeUseCase(any()) } returns true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkNotice("1.0.0")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showNoticeDialog)
    }

    @Test
    fun `checkNotice 호출 시 checkNoticeUseCase가 false이면 showNoticeDialog가 false로 유지됨`() = runTest {
        coEvery { checkNoticeUseCase(any()) } returns false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkNotice("1.0.0")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoticeDialog)
    }
}
