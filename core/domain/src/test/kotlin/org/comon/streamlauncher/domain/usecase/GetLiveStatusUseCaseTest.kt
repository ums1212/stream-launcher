package org.comon.streamlauncher.domain.usecase

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.domain.repository.FeedRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetLiveStatusUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var useCase: GetLiveStatusUseCase

    @Before
    fun setUp() {
        feedRepository = mockk()
        useCase = GetLiveStatusUseCase(feedRepository)
    }

    @Test
    fun `invoke - 라이브 중일 때 isLive=true 반환`() = runTest {
        val liveStatus = LiveStatus(
            isLive = true,
            title = "라이브 방송 중",
            viewerCount = 1234,
            thumbnailUrl = "https://thumb.chzzk.naver.com/live.jpg",
            channelId = "abc123",
        )
        every { feedRepository.getLiveStatus("abc123") } returns flowOf(Result.success(liveStatus))

        useCase("abc123").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull()?.isLive)
            assertEquals(1234, result.getOrNull()?.viewerCount)
            awaitComplete()
        }
    }

    @Test
    fun `invoke - API 에러 시 Result failure 반환`() = runTest {
        val error = RuntimeException("네트워크 오류")
        every { feedRepository.getLiveStatus("abc123") } returns flowOf(Result.failure(error))

        useCase("abc123").test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            assertEquals("네트워크 오류", result.exceptionOrNull()?.message)
            awaitComplete()
        }
    }

    @Test
    fun `invoke - feedRepository의 getLiveStatus 호출됨`() = runTest {
        val liveStatus = LiveStatus(false, "", 0, "", "ch1")
        every { feedRepository.getLiveStatus("ch1") } returns flowOf(Result.success(liveStatus))

        useCase("ch1").test { awaitItem(); awaitComplete() }
        verify { feedRepository.getLiveStatus("ch1") }
    }
}
