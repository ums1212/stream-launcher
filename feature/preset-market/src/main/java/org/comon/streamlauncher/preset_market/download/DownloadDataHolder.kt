package org.comon.streamlauncher.preset_market.download

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadDataHolder @Inject constructor() {
    var pendingPreset: MarketPreset? = null

    fun clear() {
        pendingPreset = null
    }
}
