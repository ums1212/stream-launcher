package org.comon.streamlauncher.settings.upload

import org.comon.streamlauncher.domain.model.DataHolder
import org.comon.streamlauncher.domain.model.preset.Preset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadDataHolder @Inject constructor() : DataHolder {
    var pendingPreset: Preset? = null
    var pendingPreviewUris: List<String> = emptyList()
    var pendingDescription: String = ""
    var pendingTags: List<String> = emptyList()

    override fun clear() {
        pendingPreset = null
        pendingPreviewUris = emptyList()
        pendingDescription = ""
        pendingTags = emptyList()
    }
}
