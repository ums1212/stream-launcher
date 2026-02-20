package org.comon.streamlauncher.domain.util

object ChosungMatcher {

    private val CHOSUNG = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    )

    private const val HANGUL_START = 0xAC00
    private const val HANGUL_END = 0xD7A3
    private const val CHOSUNG_COUNT = 21 * 28 // 중성(21) × 종성(28)

    /**
     * 한글 완성형 문자에서 초성만 추출한다.
     * - 한글 완성형(U+AC00~U+D7A3): 초성 인덱스 계산 후 CHOSUNG 배열에서 조회
     * - 단일 자모(U+3131~U+314E) 및 비한글: 그대로 통과
     */
    fun extractChosung(text: String): String = buildString {
        for (ch in text) {
            if (ch.code in HANGUL_START..HANGUL_END) {
                val index = (ch.code - HANGUL_START) / CHOSUNG_COUNT
                append(CHOSUNG[index])
            } else {
                append(ch)
            }
        }
    }

    /**
     * 라벨이 쿼리와 매칭되는지 판단한다.
     * - 빈 쿼리: 항상 true
     * - 쿼리가 순수 자모(ㄱ~ㅎ, U+3131~U+314E)만으로 이루어진 경우: 초성 매칭
     * - 그 외: 대소문자 무시 contains 매칭
     */
    fun matchesChosung(label: String, query: String): Boolean {
        if (query.isEmpty()) return true
        return if (query.all { it.code in 0x3131..0x314E }) {
            extractChosung(label).contains(query)
        } else {
            label.lowercase().contains(query.lowercase())
        }
    }
}
