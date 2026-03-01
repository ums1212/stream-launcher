package org.comon.streamlauncher.preset_market.navigation

import android.net.Uri

object MarketRoute {
    const val HOME = "preset_market"
    const val DETAIL = "preset_market_detail/{presetId}"
    const val SEARCH = "preset_market_search/{query}"

    fun detail(presetId: String) = "preset_market_detail/$presetId"
    fun search(query: String) = "preset_market_search/${Uri.encode(query)}"
}
