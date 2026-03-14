package org.comon.streamlauncher.domain.repository

import org.comon.streamlauncher.domain.model.preset.MarketPreset

data class PackedPresetResult(
    val slpFilePath: String,
    val presetTemplate: MarketPreset, // schemaVersion=2, slpStorageUrl/thumbnailUrl 미정
)

interface PresetPackager {
    fun packPreset(preset: MarketPreset, previewUris: List<String>, presetId: String): PackedPresetResult
    fun deleteTempFile(filePath: String)
}
