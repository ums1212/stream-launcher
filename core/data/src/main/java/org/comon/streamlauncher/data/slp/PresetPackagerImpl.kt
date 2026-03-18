package org.comon.streamlauncher.data.slp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.PackedPresetResult
import org.comon.streamlauncher.domain.repository.PresetPackager
import java.io.File
import javax.inject.Inject

class PresetPackagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PresetPackager {

    override fun packPreset(
        localPreset: Preset,
        previewUris: List<String>,
        presetId: String,
        description: String,
        tags: List<String>,
        authorUid: String,
        authorDisplayName: String,
    ): PackedPresetResult {
        val outDir = File(context.cacheDir, "slp_temp")
        val slpFile = SlpPacker.pack(context, localPreset, previewUris, outDir, presetId, description, tags, authorUid, authorDisplayName)
        val manifest = SlpPacker.buildManifest(localPreset, previewUris, description, tags, authorUid, authorDisplayName)
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
