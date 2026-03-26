package org.comon.streamlauncher.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    // --- Chzzk 채널 ID ---

    @Test
    fun `유효한 Chzzk 채널 ID - 32자리 소문자 16진수`() {
        assertTrue(InputValidator.isValidChzzkChannelId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"))
    }

    @Test
    fun `유효한 Chzzk 채널 ID - 모두 숫자`() {
        assertTrue(InputValidator.isValidChzzkChannelId("12345678901234567890123456789012"))
    }

    @Test
    fun `유효한 Chzzk 채널 ID - 모두 소문자 a-f`() {
        assertTrue(InputValidator.isValidChzzkChannelId("abcdefabcdefabcdefabcdefabcdefab"))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 빈 문자열`() {
        assertFalse(InputValidator.isValidChzzkChannelId(""))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 31자리 (짧음)`() {
        assertFalse(InputValidator.isValidChzzkChannelId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d"))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 33자리 (김)`() {
        assertFalse(InputValidator.isValidChzzkChannelId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d45"))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 대문자 포함`() {
        assertFalse(InputValidator.isValidChzzkChannelId("A1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 경로 조작 시도`() {
        assertFalse(InputValidator.isValidChzzkChannelId("../../etc/passwd/aaaaaaaaaaaaaaa"))
    }

    @Test
    fun `유효하지 않은 Chzzk 채널 ID - 특수문자 포함`() {
        assertFalse(InputValidator.isValidChzzkChannelId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d!"))
    }

    // --- YouTube 채널 ID ---

    @Test
    fun `유효한 YouTube 채널 ID - UC로 시작하는 24자리`() {
        assertTrue(InputValidator.isValidYoutubeChannelId("UCxxxxxxxxxxxxxxxxxxxxxx"))
    }

    @Test
    fun `유효한 YouTube 채널 ID - 핸들 형식 @handle`() {
        assertTrue(InputValidator.isValidYoutubeChannelId("@channelHandle123"))
    }

    @Test
    fun `유효한 YouTube 채널 ID - 핸들 언더스코어 포함`() {
        assertTrue(InputValidator.isValidYoutubeChannelId("@channel_handle"))
    }

    @Test
    fun `유효한 YouTube 채널 ID - 핸들 점 포함`() {
        assertTrue(InputValidator.isValidYoutubeChannelId("@channel.name"))
    }

    @Test
    fun `유효하지 않은 YouTube 채널 ID - 빈 문자열`() {
        assertFalse(InputValidator.isValidYoutubeChannelId(""))
    }

    @Test
    fun `유효하지 않은 YouTube 채널 ID - UC로 시작하지만 23자리 (짧음)`() {
        assertFalse(InputValidator.isValidYoutubeChannelId("UCxxxxxxxxxxxxxxxxxxxxx"))
    }

    @Test
    fun `유효하지 않은 YouTube 채널 ID - 경로 조작 시도`() {
        assertFalse(InputValidator.isValidYoutubeChannelId("../../etc/passwd"))
    }

    @Test
    fun `유효하지 않은 YouTube 채널 ID - 공백 포함`() {
        assertFalse(InputValidator.isValidYoutubeChannelId("UC xxxxxxxxxxxxxxxxxxxx"))
    }

    @Test
    fun `유효하지 않은 YouTube 채널 ID - 핸들 특수문자 포함`() {
        assertFalse(InputValidator.isValidYoutubeChannelId("@channel!handle"))
    }

    @Test
    fun `유효한 YouTube 채널 ID - 한글 핸들`() {
        assertTrue(InputValidator.isValidYoutubeChannelId("@이오몽"))
    }
}
