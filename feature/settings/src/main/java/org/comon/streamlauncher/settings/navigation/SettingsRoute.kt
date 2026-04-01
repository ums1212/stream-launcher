package org.comon.streamlauncher.settings.navigation

import kotlinx.serialization.Serializable

sealed interface LauncherRoute

@Serializable
object Launcher: LauncherRoute

@Serializable
data class SettingsDetail(val menu: String): LauncherRoute

@Serializable
object PresetMarketHost: LauncherRoute

@Serializable
object AddNewPreset : LauncherRoute

enum class SettingsMenu { COLOR, IMAGE, FEED, APP_DRAWER, PRESET, LIVE_WALLPAPER, SUGGESTION, STATIC_WALLPAPER }
