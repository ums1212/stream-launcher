package org.comon.streamlauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPresetsTest {

    @Test
    fun `프리셋 개수는 7개`() {
        assertEquals(7, ColorPresets.defaults.size)
    }

    @Test
    fun `getByIndex - 유효한 인덱스 반환`() {
        for (i in 0..6) {
            assertEquals(i, ColorPresets.getByIndex(i).index)
        }
    }

    @Test
    fun `getByIndex - 범위 초과 시 0번 프리셋 폴백`() {
        assertEquals(ColorPresets.defaults[0], ColorPresets.getByIndex(99))
    }

    @Test
    fun `getByIndex - 음수 인덱스 시 0번 프리셋 폴백`() {
        assertEquals(ColorPresets.defaults[0], ColorPresets.getByIndex(-1))
    }

    @Test
    fun `모든 프리셋의 accentPrimaryArgb가 고유함`() {
        val primaries = ColorPresets.defaults.map { it.accentPrimaryArgb }
        assertEquals(primaries.size, primaries.distinct().size)
    }

    @Test
    fun `모든 프리셋의 accentSecondaryArgb가 고유함`() {
        val secondaries = ColorPresets.defaults.map { it.accentSecondaryArgb }
        assertEquals(secondaries.size, secondaries.distinct().size)
    }

    @Test
    fun `각 프리셋 이름이 고유함`() {
        val names = ColorPresets.defaults.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }
}
