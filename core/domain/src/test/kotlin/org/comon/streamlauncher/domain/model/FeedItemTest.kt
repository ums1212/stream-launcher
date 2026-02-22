package org.comon.streamlauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedItemTest {

    @Test
    fun `NoticeItem 생성 - 필드 정확히 설정됨`() {
        val item = FeedItem.NoticeItem(
            title = "공지 제목",
            dateMillis = 1_700_000_000_000L,
            link = "https://cafe.naver.com/notice/1",
            source = "네이버 카페",
        )
        assertEquals("공지 제목", item.title)
        assertEquals(1_700_000_000_000L, item.dateMillis)
        assertEquals("https://cafe.naver.com/notice/1", item.link)
        assertEquals("네이버 카페", item.source)
    }

    @Test
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
    fun `FeedItem sealed interface - NoticeItem과 VideoItem 다형성`() {
        val items: List<FeedItem> = listOf(
            FeedItem.NoticeItem("공지", 100L, "https://link.com", "source"),
            FeedItem.VideoItem("영상", 200L, "https://thumb.com", "https://yt.com/watch?v=x"),
        )
        assertTrue(items[0] is FeedItem.NoticeItem)
        assertTrue(items[1] is FeedItem.VideoItem)
    }

    @Test
    fun `dateMillis 기준 내림차순 정렬`() {
        val items: List<FeedItem> = listOf(
            FeedItem.NoticeItem("오래된 공지", 100L, "", ""),
            FeedItem.VideoItem("최신 영상", 300L, "", ""),
            FeedItem.NoticeItem("중간 공지", 200L, "", ""),
        )
        val sorted = items.sortedByDescending { it.dateMillis }
        assertEquals(300L, sorted[0].dateMillis)
        assertEquals(200L, sorted[1].dateMillis)
        assertEquals(100L, sorted[2].dateMillis)
    }

    @Test
    fun `FeedItem when 분기 처리`() {
        val notice: FeedItem = FeedItem.NoticeItem("제목", 0L, "link", "src")
        val video: FeedItem = FeedItem.VideoItem("제목", 0L, "thumb", "vid")

        val noticeResult = when (notice) {
            is FeedItem.NoticeItem -> "notice"
            is FeedItem.VideoItem -> "video"
        }
        val videoResult = when (video) {
            is FeedItem.NoticeItem -> "notice"
            is FeedItem.VideoItem -> "video"
        }
        assertEquals("notice", noticeResult)
        assertEquals("video", videoResult)
    }
}
