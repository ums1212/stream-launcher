package org.comon.streamlauncher.settings.feed

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
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var saveFeedSettingsUseCase: SaveFeedSettingsUseCase
    private lateinit var viewModel: FeedSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        saveFeedSettingsUseCase = mockk(relaxed = true)
        viewModel = makeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(
        settingsUseCase: GetLauncherSettingsUseCase = getLauncherSettingsUseCase,
        feedUseCase: SaveFeedSettingsUseCase = saveFeedSettingsUseCase,
    ) = FeedSettingsViewModel(settingsUseCase, feedUseCase)

    @Test
    fun `SaveFeedSettings 처리 시 설정 상태가 업데이트되고 유스케이스가 호출됨`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(
            FeedSettingsIntent.SaveFeedSettings(
                chzzkChannelId = "chzzk123",
                youtubeChannelId = "yt456",
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("chzzk123", state.chzzkChannelId)
        assertEquals("yt456", state.youtubeChannelId)

        coVerify { saveFeedSettingsUseCase("chzzk123", "yt456") }
    }
}
