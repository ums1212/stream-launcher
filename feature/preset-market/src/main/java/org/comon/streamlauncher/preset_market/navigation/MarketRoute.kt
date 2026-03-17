package org.comon.streamlauncher.preset_market.navigation

import kotlinx.serialization.Serializable

sealed interface MarketRoute

@Serializable
object MarketHome: MarketRoute

@Serializable
data class MarketDetail(val presetId: String): MarketRoute

@Serializable
data class MarketSearch(val query: String = ""): MarketRoute
