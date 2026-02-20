package org.comon.streamlauncher.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChosungMatcherTest {

    // 1. 한글 초성 추출
    @Test
    fun `extractChosung - 한글 완성형에서 초성 추출`() {
        assertEquals("ㅋㅋㅇㅌ", ChosungMatcher.extractChosung("카카오톡"))
    }

    // 2. 비한글 그대로 통과
    @Test
    fun `extractChosung - 비한글은 그대로 통과`() {
        assertEquals("ABC123", ChosungMatcher.extractChosung("ABC123"))
    }

    // 3. 빈 문자열
    @Test
    fun `extractChosung - 빈 문자열은 빈 문자열 반환`() {
        assertEquals("", ChosungMatcher.extractChosung(""))
    }

    // 4. 한글+영문 혼합
    @Test
    fun `extractChosung - 한글과 영문 혼합 처리`() {
        assertEquals("ㅎㄱMixㅎㅎ", ChosungMatcher.extractChosung("한글Mix혼합"))
    }

    // 5. 단일 자모 입력 (U+3131~U+314E)는 그대로 통과
    @Test
    fun `extractChosung - 단일 자모는 변환 없이 그대로 통과`() {
        assertEquals("ㄱㄴㄷ", ChosungMatcher.extractChosung("ㄱㄴㄷ"))
    }

    // 6. 초성 부분 매칭 (query가 자모)
    @Test
    fun `matchesChosung - 초성 부분 매칭이 동작함`() {
        assertTrue(ChosungMatcher.matchesChosung("카카오톡", "ㅋㅋ"))
    }

    // 7. 완성형 부분 매칭
    @Test
    fun `matchesChosung - 완성형 부분 매칭이 동작함`() {
        assertTrue(ChosungMatcher.matchesChosung("카카오톡", "카카"))
    }

    // 8. 순서 불일치 → false
    @Test
    fun `matchesChosung - 순서 불일치 시 false 반환`() {
        assertFalse(ChosungMatcher.matchesChosung("카카오톡", "ㅌㅋ"))
    }

    // 9. 영문 대소문자 무시 contains
    @Test
    fun `matchesChosung - 영문 대소문자 무시 검색`() {
        assertTrue(ChosungMatcher.matchesChosung("Settings", "set"))
    }

    // 10. 영문 소문자로 매칭
    @Test
    fun `matchesChosung - 영문 소문자로 대문자 앱 매칭`() {
        assertTrue(ChosungMatcher.matchesChosung("YouTube", "youtube"))
    }

    // 11. 빈 쿼리 → true (전체 반환)
    @Test
    fun `matchesChosung - 빈 쿼리는 항상 true`() {
        assertTrue(ChosungMatcher.matchesChosung("카카오톡", ""))
        assertTrue(ChosungMatcher.matchesChosung("YouTube", ""))
        assertTrue(ChosungMatcher.matchesChosung("", ""))
    }
}
