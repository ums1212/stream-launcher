package org.comon.streamlauncher.data.slp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.PackedPresetResult
import org.comon.streamlauncher.domain.repository.PresetPackager
import java.io.File
import javax.inject.Inject

class PresetPackagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PresetPackager {

    override fun packPreset(
        preset: MarketPreset,
        previewUris: List<String>,
        presetId: String,
    ): PackedPresetResult {
        val outDir = File(context.cacheDir, "slp_temp")
        val slpFile = SlpPacker.pack(context, preset, previewUris, outDir, presetId)
        val manifest = SlpPacker.buildManifest(preset, previewUris)
        val presetTemplate = manifest.toMarketPreset(presetId, "", "")
        return PackedPresetResult(
            slpFilePath = slpFile.absolutePath,
            presetTemplate = presetTemplate,
        )
    }

    override fun deleteTempFile(filePath: String) {
        File(filePath).delete()
    }
}
