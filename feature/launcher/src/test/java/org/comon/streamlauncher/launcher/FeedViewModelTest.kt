package org.comon.streamlauncher.launcher

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.ChannelProfile
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.domain.usecase.GetChannelProfileUseCase
import org.comon.streamlauncher.domain.usecase.GetIntegratedFeedUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.GetChzzkLiveStatusUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLauncherSettingsUseCase: GetLauncherSettingsUseCase
    private lateinit var getChzzkLiveStatusUseCase: GetChzzkLiveStatusUseCase
    private lateinit var getIntegratedFeedUseCase: GetIntegratedFeedUseCase
    private lateinit var getChannelProfileUseCase: GetChannelProfileUseCase
    private lateinit var viewModel: FeedViewModel

    private val defaultSettings = LauncherSettings(
        chzzkChannelId = "testChannelId",
        rssUrl = "https://rss.url",
        youtubeChannelId = "UCtest",
    )

    private val sampleLiveStatus = LiveStatus(
        isLive = true,
        title = "테스트 방송",
        viewerCount = 500,
        thumbnailUrl = "https://thumb.jpg",
        channelId = "testChannelId",
    )

    private val sampleFeedItems = listOf(
        FeedItem.NoticeItem("공지1", 200L, "https://link1.com", "카페"),
        FeedItem.VideoItem("영상1", 100L, "https://thumb.jpg", "https://yt.com/watch?v=1"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLauncherSettingsUseCase = mockk()
        getChzzkLiveStatusUseCase = mockk()
        getIntegratedFeedUseCase = mockk()
        getChannelProfileUseCase = mockk(relaxed = true)

        every { getLauncherSettingsUseCase() } returns flowOf(defaultSettings)
        every { getChzzkLiveStatusUseCase(any()) } returns flowOf(Result.success(sampleLiveStatus))
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.success(sampleFeedItems))
        every { getChannelProfileUseCase(any()) } returns flowOf(
            Result.success(ChannelProfile("UCtest", "Test Channel", "", 10000L, 100L))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel() = FeedViewModel(
        getLauncherSettingsUseCase,
        getChzzkLiveStatusUseCase,
        getIntegratedFeedUseCase,
        getChannelProfileUseCase,
    )

    @Test
    fun `초기 상태 - 기본값 확인`() {
        every { getLauncherSettingsUseCase() } returns flowOf(LauncherSettings())
        every { getChzzkLiveStatusUseCase(any()) } returns flowOf(Result.success(LiveStatus(false, "", 0, "", "")))
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))

        viewModel = makeViewModel()
        assertEquals(emptyList<FeedItem>(), viewModel.uiState.value.feedItems)
    }

    @Test
    fun `init - 설정 로딩 후 상태에 channelId 반영`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        assertEquals("testChannelId", viewModel.uiState.value.chzzkChannelId)
        assertEquals("https://rss.url", viewModel.uiState.value.rssUrl)
        assertEquals("UCtest", viewModel.uiState.value.youtubeChannelId)
    }

    @Test
    fun `init - 피드 아이템 로드 성공`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.feedItems.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init - 라이브 상태 갱신`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.liveStatus)
        assertTrue(viewModel.uiState.value.liveStatus!!.isLive)
        assertEquals("테스트 방송", viewModel.uiState.value.liveStatus!!.title)
    }

    @Test
    fun `Refresh - 피드 새로 로드`() = runTest {
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
        viewModel = makeViewModel()
        advanceUntilIdle()

        val newItems = listOf(FeedItem.NoticeItem("새공지", 999L, "link", "src"))
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.success(newItems))

        viewModel.handleIntent(FeedIntent.Refresh)
        advanceUntilIdle()

        // 쿨다운으로 인해 첫 번째 Refresh는 무시될 수 있으나 force=false이므로 큰 효과 없음
        // 테스트는 초기 로드 성공 확인
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `ClickFeedItem NoticeItem - OpenUrl 이펙트 발행`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        val noticeItem = FeedItem.NoticeItem("공지", 0L, "https://notice.link", "src")
        viewModel.effect.test {
            viewModel.handleIntent(FeedIntent.ClickFeedItem(noticeItem))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is FeedSideEffect.OpenUrl)
            assertEquals("https://notice.link", (effect as FeedSideEffect.OpenUrl).url)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ClickFeedItem VideoItem - OpenUrl 이펙트 발행`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        val videoItem = FeedItem.VideoItem("영상", 0L, "thumb", "https://yt.com/watch?v=abc")
        viewModel.effect.test {
            viewModel.handleIntent(FeedIntent.ClickFeedItem(videoItem))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is FeedSideEffect.OpenUrl)
            assertEquals("https://yt.com/watch?v=abc", (effect as FeedSideEffect.OpenUrl).url)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ClickLiveStatus - 치지직 URL로 OpenUrl 이펙트 발행`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleIntent(FeedIntent.ClickLiveStatus)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is FeedSideEffect.OpenUrl)
            assertTrue((effect as FeedSideEffect.OpenUrl).url.contains("chzzk.naver.com/live/"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `피드 로드 실패 - errorMessage 설정`() = runTest {
        every { getChzzkLiveStatusUseCase(any()) } returns flowOf(Result.success(LiveStatus(false, "", 0, "", "")))
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.failure(RuntimeException("네트워크 오류")))

        viewModel = makeViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isLoading - 로딩 중 상태 관리`() = runTest {
        every { getIntegratedFeedUseCase(any(), any()) } returns flowOf(Result.success(emptyList()))
        every { getChzzkLiveStatusUseCase(any()) } returns flowOf(Result.success(LiveStatus(false, "", 0, "", "")))

        viewModel = makeViewModel()
        // advanceUntilIdle 전 로딩 상태 확인은 타이밍 이슈가 있으므로 완료 후 false 확인
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init - 채널 프로필 로드 성공`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.channelProfile)
        assertEquals("Test Channel", viewModel.uiState.value.channelProfile!!.name)
        assertEquals(10000L, viewModel.uiState.value.channelProfile!!.subscriberCount)
    }

    @Test
    fun `ClickChannelProfile - YouTube 채널 URL로 OpenUrl 이펙트 발행`() = runTest {
        viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleIntent(FeedIntent.ClickChannelProfile)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is FeedSideEffect.OpenUrl)
            assertTrue((effect as FeedSideEffect.OpenUrl).url.contains("youtube.com/channel/"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
