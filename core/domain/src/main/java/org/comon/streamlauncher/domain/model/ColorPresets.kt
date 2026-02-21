package org.comon.streamlauncher.domain.model

object ColorPresets {
    val defaults: List<ColorPreset> = listOf(
        ColorPreset(0, "Purple Dream", 0xFF9B8FFFL, 0xFFFF8FB1L),
        ColorPreset(1, "Ocean Blue",   0xFF64B5F6L, 0xFF80DEEAL),
        ColorPreset(2, "Sunset Orange",0xFFFF8A65L, 0xFFFFD54FL),
        ColorPreset(3, "Mint Green",   0xFF69F0AEL, 0xFFA5D6A7L),
        ColorPreset(4, "Cherry Pink",  0xFFFF6090L, 0xFFCE93D8L),
        ColorPreset(5, "Gold Royal",   0xFFFFD700L, 0xFFFFA726L),
        ColorPreset(6, "Neon Cyber",   0xFF00E5FFL, 0xFFE040FBL),
    )

    fun getByIndex(index: Int): ColorPreset = defaults.getOrElse(index) { defaults[0] }
}
