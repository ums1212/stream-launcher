package org.comon.streamlauncher.domain.repository

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset

data class PackedPresetResult(
    val slpFilePath: String,
    val presetTemplate: MarketPreset, // schemaVersion=2, slpStorageUrl/thumbnailUrl 미정
)

interface PresetPackager {
    fun packPreset(
        localPreset: Preset,
        previewUris: List<String>,
        presetId: String,
        description: String = "",
        tags: List<String> = emptyList(),
        authorUid: String = "",
        authorDisplayName: String = "",
    ): PackedPresetResult
    fun deleteTempFile(filePath: String)
}
