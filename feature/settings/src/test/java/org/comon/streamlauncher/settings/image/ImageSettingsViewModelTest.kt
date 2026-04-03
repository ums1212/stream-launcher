package org.comon.streamlauncher.settings.image

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
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import org.comon.streamlauncher.settings.model.ImageType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveGridCellImageUseCase: SaveGridCellImageUseCase
    private lateinit var viewModel: ImageSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        saveGridCellImageUseCase = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        imageSaveUseCase: SaveGridCellImageUseCase = saveGridCellImageUseCase,
    ) = ImageSettingsViewModel(settingsUseCase, imageSaveUseCase)

    @Test
    fun `SetGridImage IDLE 처리 시 해당 셀의 idleImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/1"
        viewModel.handleIntent(ImageSettingsIntent.SetGridImage(GridCell.TOP_LEFT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.TOP_LEFT]
        assertEquals(testUri, cellImage?.idleImageUri)
    }

    @Test
    fun `SetGridImage EXPANDED 처리 시 해당 셀의 expandedImageUri가 업데이트됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/2"
        viewModel.handleIntent(ImageSettingsIntent.SetGridImage(GridCell.BOTTOM_RIGHT, ImageType.EXPANDED, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val cellImage = viewModel.uiState.value.gridCellImages[GridCell.BOTTOM_RIGHT]
        assertEquals(testUri, cellImage?.expandedImageUri)
    }

    @Test
    fun `SetGridImage 처리 시 saveGridCellImageUseCase가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val testUri = "content://media/external/images/3"
        viewModel.handleIntent(ImageSettingsIntent.SetGridImage(GridCell.TOP_RIGHT, ImageType.IDLE, testUri))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { saveGridCellImageUseCase(GridCell.TOP_RIGHT, testUri, null) }
    }
}
