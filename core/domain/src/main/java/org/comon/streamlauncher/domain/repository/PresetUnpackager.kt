package org.comon.streamlauncher.domain.repository

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset

data class UnpackedPresetResult(
    val localPreset: Preset,
)

interface PresetUnpackager {
    /** .slp 다운로드 + 추출 → 로컬 Preset */
    suspend fun downloadAndUnpack(marketPreset: MarketPreset): UnpackedPresetResult

    /** 실패 시 로컬 파일 정리 */
    fun cleanupPresetDir(marketPresetId: String)
}
