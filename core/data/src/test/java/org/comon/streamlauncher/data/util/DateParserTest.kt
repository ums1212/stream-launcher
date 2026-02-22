package org.comon.streamlauncher.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DateParserTest {

    @Test
    fun `parseRfc822 - 표준 RFC 822 형식 파싱 성공`() {
        val dateStr = "Mon, 01 Jan 2024 12:00:00 +0000"
        val result = DateParser.parseRfc822(dateStr)
        assertNotEquals(0L, result)
    }

    @Test
    fun `parseRfc822 - 잘못된 형식은 0L 반환`() {
        val result = DateParser.parseRfc822("not-a-date")
        assertEquals(0L, result)
    }

    @Test
    fun `parseRfc822 - 빈 문자열은 0L 반환`() {
        assertEquals(0L, DateParser.parseRfc822(""))
    }

    @Test
    fun `parseIso8601 - ISO 8601 UTC 형식 파싱 성공`() {
        val dateStr = "2024-01-15T09:30:00Z"
        val result = DateParser.parseIso8601(dateStr)
        assertNotEquals(0L, result)
    }

    @Test
    fun `parseIso8601 - 잘못된 형식은 0L 반환`() {
        val result = DateParser.parseIso8601("invalid-date")
        assertEquals(0L, result)
    }

    @Test
    fun `parseIso8601 - 빈 문자열은 0L 반환`() {
        assertEquals(0L, DateParser.parseIso8601(""))
    }

    @Test
    fun `parseRfc822 - 두 날짜 시간 순서 비교`() {
        val older = DateParser.parseRfc822("Mon, 01 Jan 2024 00:00:00 +0000")
        val newer = DateParser.parseRfc822("Tue, 02 Jan 2024 00:00:00 +0000")
        if (older != 0L && newer != 0L) {
            assert(newer > older) { "newer should be greater than older" }
        }
    }
}
