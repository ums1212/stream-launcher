package org.comon.streamlauncher.settings.upload

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadDataHolder @Inject constructor() {
    var pendingPreset: MarketPreset? = null
    var pendingPreviewUris: List<String> = emptyList()

    fun clear() {
        pendingPreset = null
        pendingPreviewUris = emptyList()
    }
}
