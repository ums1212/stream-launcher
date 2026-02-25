package org.comon.streamlauncher.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.network.api.ChzzkService
import org.comon.streamlauncher.network.api.RssFeedApi
import org.comon.streamlauncher.network.api.YouTubeService
import org.comon.streamlauncher.network.model.ChzzkLiveContent
import org.comon.streamlauncher.network.model.ChzzkLiveResponse
import org.comon.streamlauncher.network.model.RssChannel
import org.comon.streamlauncher.network.model.RssFeedResponse
import org.comon.streamlauncher.network.model.RssItem
import org.comon.streamlauncher.network.model.YouTubeSearchItem
import org.comon.streamlauncher.network.model.YouTubeSearchResponse
import org.comon.streamlauncher.network.model.YouTubeSnippet
import org.comon.streamlauncher.network.model.YouTubeThumbnails
import org.comon.streamlauncher.network.model.YouTubeVideoId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var chzzkService: ChzzkService
    private lateinit var youTubeService: YouTubeService
    private lateinit var rssFeedApi: RssFeedApi
    private lateinit var cacheDataStore: DataStore<Preferences>
    private lateinit var repository: FeedRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        chzzkService = mockk()
        youTubeService = mockk()
        rssFeedApi = mockk()

        // DataStore relaxed mock: updateData (called by edit) returns emptyPreferences()
        cacheDataStore = mockk(relaxed = true)
        every { cacheDataStore.data } returns flowOf(emptyPreferences())
        coEvery { cacheDataStore.updateData(any()) } returns emptyPreferences()

        repository = FeedRepositoryImpl(chzzkService, youTubeService, rssFeedApi, cacheDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getLiveStatus - OPEN 상태일 때 isLive=true`() = runTest {
        val response = ChzzkLiveResponse(
            code = 200,
            content = ChzzkLiveContent(
                status = "OPEN",
                liveTitle = "방송 중",
                concurrentUserCount = 999,
                liveImageUrl = "https://thumb.jpg",
            ),
        )
        coEvery { chzzkService.getLiveDetail(any()) } returns response

        repository.getLiveStatus("testChannelId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val status = result.getOrThrow()
            assertTrue(status.isLive)
            assertEquals("방송 중", status.title)
            assertEquals(999, status.viewerCount)
            awaitComplete()
        }
    }

    @Test
    fun `getLiveStatus - CLOSE 상태일 때 isLive=false`() = runTest {
        val response = ChzzkLiveResponse(
            code = 200,
            content = ChzzkLiveContent(status = "CLOSE"),
        )
        coEvery { chzzkService.getLiveDetail(any()) } returns response

        repository.getLiveStatus("testChannelId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertFalse(result.getOrThrow().isLive)
            awaitComplete()
        }
    }

    @Test
    fun `getLiveStatus - 빈 channelId일 때 isLive=false 즉시 반환`() = runTest {
        repository.getLiveStatus("").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertFalse(result.getOrThrow().isLive)
            awaitComplete()
        }
        coVerify(exactly = 0) { chzzkService.getLiveDetail(any()) }
    }

    @Test
    fun `getLiveStatus - API 오류 시 Result failure`() = runTest {
        coEvery { chzzkService.getLiveDetail(any()) } throws RuntimeException("API 오류")

        repository.getLiveStatus("channelId").test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            awaitComplete()
        }
    }

    @Test
    fun `getIntegratedFeed - RSS 항목 정상 파싱`() = runTest {
        val rssResponse = RssFeedResponse(
            channel = RssChannel(
                title = "카페 공지",
                items = listOf(
                    RssItem(title = "공지1", link = "https://link.com", pubDate = "Mon, 01 Jan 2024 12:00:00 +0000"),
                    RssItem(title = "공지2", link = "https://link2.com", pubDate = "Tue, 02 Jan 2024 12:00:00 +0000"),
                ),
            ),
        )
        coEvery { rssFeedApi.getRssFeed(any()) } returns rssResponse

        repository.getIntegratedFeed("https://rss.url", "").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val items = result.getOrThrow()
            assertEquals(2, items.size)
            assertTrue(items.all { it is FeedItem.NoticeItem })
            awaitComplete()
        }
    }

    @Test
    fun `getIntegratedFeed - YouTube 항목 정상 파싱`() = runTest {
        val ytResponse = YouTubeSearchResponse(
            items = listOf(
                YouTubeSearchItem(
                    id = YouTubeVideoId("vid1"),
                    snippet = YouTubeSnippet(
                        title = "영상1",
                        publishedAt = "2024-01-15T09:00:00Z",
                        thumbnails = YouTubeThumbnails(),
                    ),
                ),
            ),
        )
        coEvery { youTubeService.searchVideos(any(), any(), any(), any(), any(), any(), any(), any()) } returns ytResponse

        repository.getIntegratedFeed("", "UCsomeChannelId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val items = result.getOrThrow()
            assertEquals(1, items.size)
            assertTrue(items.first() is FeedItem.VideoItem)
            awaitComplete()
        }
    }

    @Test
    fun `getIntegratedFeed - 한쪽 실패해도 다른쪽 결과 반환`() = runTest {
        coEvery { rssFeedApi.getRssFeed(any()) } throws RuntimeException("RSS 오류")
        val ytResponse = YouTubeSearchResponse(
            items = listOf(
                YouTubeSearchItem(
                    id = YouTubeVideoId("vid1"),
                    snippet = YouTubeSnippet("영상1", "2024-01-15T09:00:00Z", YouTubeThumbnails()),
                ),
            ),
        )
        coEvery { youTubeService.searchVideos(any(), any(), any(), any(), any(), any(), any(), any()) } returns ytResponse

        repository.getIntegratedFeed("https://rss.url", "UCsomeChannelId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val items = result.getOrThrow()
            assertEquals(1, items.size) // RSS 실패, YouTube만 성공
            awaitComplete()
        }
    }

    @Test
    fun `getIntegratedFeed - dateMillis 내림차순 정렬`() = runTest {
        val rssResponse = RssFeedResponse(
            channel = RssChannel(
                title = "source",
                items = listOf(
                    RssItem(title = "오래된공지", link = "", pubDate = "Mon, 01 Jan 2024 12:00:00 +0000"),
                ),
            ),
        )
        val ytResponse = YouTubeSearchResponse(
            items = listOf(
                YouTubeSearchItem(
                    id = YouTubeVideoId("vid1"),
                    snippet = YouTubeSnippet("최신영상", "2024-06-15T09:00:00Z", YouTubeThumbnails()),
                ),
            ),
        )
        coEvery { rssFeedApi.getRssFeed(any()) } returns rssResponse
        coEvery { youTubeService.searchVideos(any(), any(), any(), any(), any(), any(), any(), any()) } returns ytResponse

        repository.getIntegratedFeed("https://rss.url", "UCsomeId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val items = result.getOrThrow()
            assertTrue(items.first().dateMillis >= items.last().dateMillis)
            awaitComplete()
        }
    }
}
