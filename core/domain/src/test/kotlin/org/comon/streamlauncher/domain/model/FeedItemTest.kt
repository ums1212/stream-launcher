package org.comon.streamlauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedItemTest {
    fun `VideoItem 생성 - 필드 정확히 설정됨`() {
        val item = FeedItem.VideoItem(
            title = "영상 제목",
            dateMillis = 1_710_000_000_000L,
            thumbnailUrl = "https://img.youtube.com/vi/abc/hqdefault.jpg",
            videoLink = "https://www.youtube.com/watch?v=abc",
        )
        assertEquals("영상 제목", item.title)
        assertEquals(1_710_000_000_000L, item.dateMillis)
        assertEquals("https://img.youtube.com/vi/abc/hqdefault.jpg", item.thumbnailUrl)
        assertEquals("https://www.youtube.com/watch?v=abc", item.videoLink)
    }

    @Test
    fun `dateMillis 기준 내림차순 정렬`() {
        val items: List<FeedItem> = listOf(
            FeedItem.VideoItem("오래된 영상", 100L, "", ""),
            FeedItem.VideoItem("최신 영상", 300L, "", ""),
            FeedItem.VideoItem("중간 영상", 200L, "", ""),
        )
        val sorted = items.sortedByDescending { it.dateMillis }
        assertEquals(300L, sorted[0].dateMillis)
        assertEquals(200L, sorted[1].dateMillis)
        assertEquals(100L, sorted[2].dateMillis)
    }
}
