package org.comon.streamlauncher.domain.usecase

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.repository.FeedRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetIntegratedFeedUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var useCase: GetIntegratedFeedUseCase

    @Before
    fun setUp() {
        feedRepository = mockk()
        useCase = GetIntegratedFeedUseCase(feedRepository)
    }

    @Test
    fun `invoke - 통합 피드 성공 반환`() = runTest {
        val items = listOf(
            FeedItem.VideoItem("영상1", 100L, "thumb1", "vid1"),
            FeedItem.VideoItem("영상2", 200L, "thumb2", "vid2"),
        )
        every {
            feedRepository.getIntegratedFeed("UCchannelId")
        } returns flowOf(Result.success(items))

        useCase("UCchannelId").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
            awaitComplete()
        }
    }

    @Test
    fun `invoke - 에러 시 Result failure 반환`() = runTest {
        val error = RuntimeException("피드 오류")
        every { feedRepository.getIntegratedFeed("") } returns flowOf(Result.failure(error))

        useCase("").test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            awaitComplete()
        }
    }

    @Test
    fun `invoke - feedRepository의 getIntegratedFeed 정확한 파라미터로 호출됨`() = runTest {
        every {
            feedRepository.getIntegratedFeed("ytId")
        } returns flowOf(Result.success(emptyList()))

        useCase("ytId").test { awaitItem(); awaitComplete() }
        verify { feedRepository.getIntegratedFeed("ytId") }
    }
}
