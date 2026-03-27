package org.comon.streamlauncher.preset_market.navigation

import kotlinx.serialization.Serializable

sealed interface MarketRoute

@Serializable
object MarketHome: MarketRoute

@Serializable
data class MarketDetail(val presetId: String, val fromCard: Boolean = false): MarketRoute

@Serializable
data class MarketSearch(val query: String = ""): MarketRoute

@Serializable
object MarketUserInfo: MarketRoute

@Serializable
data class MarketReport(
    val presetId: String,
    val presetName: String,
    val presetAuthorUid: String,
    val presetAuthorDisplayName: String,
): MarketRoute
