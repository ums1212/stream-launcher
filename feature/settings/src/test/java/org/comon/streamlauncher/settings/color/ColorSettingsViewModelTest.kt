package org.comon.streamlauncher.settings.color

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
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ColorSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveColorPresetUseCase: SaveColorPresetUseCase
    private lateinit var viewModel: ColorSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        saveColorPresetUseCase = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        colorSaveUseCase: SaveColorPresetUseCase = saveColorPresetUseCase,
    ) = ColorSettingsViewModel(settingsUseCase, colorSaveUseCase)

    @Test
    fun `ChangeAccentColor 처리 시 colorPresetIndex 상태가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(ColorSettingsIntent.ChangeAccentColor(3))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.colorPresetIndex)
    }

    @Test
    fun `ChangeAccentColor 처리 시 saveColorPresetUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(ColorSettingsIntent.ChangeAccentColor(5))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveColorPresetUseCase(5) }
    }

    @Test
    fun `초기 설정 로드 시 colorPresetIndex가 저장된 값으로 복원됨`() = runTest {
        val savedSettings = LauncherSettings(colorPresetIndex = 4)
        val settingsUseCase: GetLauncherSettingsUseCase = mockk()
        every { settingsUseCase() } returns flowOf(savedSettings)

        val restoredVm = makeViewModel(settingsUseCase = settingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, restoredVm.uiState.value.colorPresetIndex)
    }
}
