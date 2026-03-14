package org.comon.streamlauncher.domain.repository

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset

data class UnpackedPresetResult(
    val localPreset: Preset,
)

interface PresetUnpackager {
    /** V2: .slp 다운로드 + 추출 → 로컬 Preset */
    suspend fun downloadAndUnpack(marketPreset: MarketPreset): UnpackedPresetResult

    /** V1(레거시): 개별 이미지 다운로드 → 로컬 Preset */
    suspend fun downloadLegacyImages(marketPreset: MarketPreset): UnpackedPresetResult

    /** 실패 시 로컬 파일 정리 */
    fun cleanupPresetDir(marketPresetId: String)
}
