package org.comon.streamlauncher.data.slp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetUnpackager
import org.comon.streamlauncher.domain.repository.UnpackedPresetResult
import java.io.File
import javax.inject.Inject

class PresetUnpackagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val marketRepository: MarketPresetRepository,
) : PresetUnpackager {

    override suspend fun downloadAndUnpack(marketPreset: MarketPreset): UnpackedPresetResult {
        val presetDir = File(context.filesDir, "market_presets/${marketPreset.id}")
        val cacheFile = File(context.cacheDir, "slp_download/${marketPreset.id}.slp")
        cacheFile.parentFile?.mkdirs()

        try {
            marketRepository.downloadSlpFile(marketPreset.slpStorageUrl!!, cacheFile.absolutePath).getOrThrow()
            val (manifest, extractedPaths) = SlpUnpacker.unpack(cacheFile, presetDir)
            val localPreset = manifest.toLocalPreset(extractedPaths, marketPreset.id)
            return UnpackedPresetResult(localPreset)
        } finally {
            cacheFile.delete()
        }
    }

    override fun cleanupPresetDir(marketPresetId: String) {
        File(context.filesDir, "market_presets/$marketPresetId").deleteRecursively()
    }
}
