package org.comon.streamlauncher.settings.appdrawer

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
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase
    private lateinit var viewModel: AppDrawerSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        saveAppDrawerSettingsUseCase = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        appDrawerUseCase: SaveAppDrawerSettingsUseCase = saveAppDrawerSettingsUseCase,
    ) = AppDrawerSettingsViewModel(settingsUseCase, appDrawerUseCase)

    @Test
    fun `SaveAppDrawerSettings 처리 시 설정 상태가 업데이트되고 유스케이스가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(AppDrawerSettingsIntent.SaveAppDrawerSettings(5, 7, 1.2f))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.appDrawerGridColumns)
        assertEquals(7, state.appDrawerGridRows)
        assertEquals(1.2f, state.appDrawerIconSizeRatio, 0.0f)

        coVerify { saveAppDrawerSettingsUseCase(5, 7, 1.2f) }
    }
}
