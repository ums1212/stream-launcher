package org.comon.streamlauncher.settings.navigation

object SettingsRoute {
    const val LAUNCHER = "launcher"
    const val DETAIL = "settings_detail/{menu}"
    fun detail(menu: String) = "settings_detail/$menu"
}

enum class SettingsMenu { COLOR, IMAGE, FEED, APP_DRAWER, PRESET }
