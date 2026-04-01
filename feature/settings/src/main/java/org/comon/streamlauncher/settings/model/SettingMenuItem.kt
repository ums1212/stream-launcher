package org.comon.streamlauncher.settings.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector
import org.comon.streamlauncher.settings.R

// 각 메뉴의 동작을 구분할 고유 식별자
enum class SettingsActionType {
    COLOR, IMAGE, APP_DRAWER, FEED, NOTICE, WALLPAPER, DEFAULT_HOME, PRESET, LIVE_WALLPAPER, SUGGESTION, STATIC_WALLPAPER
}

data class SettingMenuItem(
    val labelId: Int,
    val icon: ImageVector,
    val lerpFraction: Float,
    val actionType: SettingsActionType,
)

val settingMenuList = listOf(
    // 외관
    SettingMenuItem(
        labelId = R.string.settings_theme_color,
        icon = Icons.Rounded.Palette,
        lerpFraction = 0f,
        actionType = SettingsActionType.COLOR
    ),
    SettingMenuItem(
        labelId = R.string.settings_home_image,
        icon = Icons.Rounded.Image,
        lerpFraction = 0.11f,
        actionType = SettingsActionType.IMAGE
    ),
    SettingMenuItem(
        labelId = R.string.settings_wallpaper,
        icon = Icons.Rounded.Wallpaper,
        lerpFraction = 0.22f,
        actionType = SettingsActionType.STATIC_WALLPAPER
    ),
    SettingMenuItem(
        labelId = R.string.settings_live_wallpaper,
        icon = Icons.Rounded.SlowMotionVideo,
        lerpFraction = 0.33f,
        actionType = SettingsActionType.LIVE_WALLPAPER
    ),
    // 콘텐츠
    SettingMenuItem(
        labelId = R.string.settings_app_drawer,
        icon = Icons.Rounded.GridView,
        lerpFraction = 0.44f,
        actionType = SettingsActionType.APP_DRAWER
    ),
    SettingMenuItem(
        labelId = R.string.settings_feed,
        icon = Icons.Rounded.LiveTv,
        lerpFraction = 0.56f,
        actionType = SettingsActionType.FEED
    ),
    // 관리
    SettingMenuItem(
        labelId = R.string.settings_preset,
        icon = Icons.Rounded.Save,
        lerpFraction = 0.67f,
        actionType = SettingsActionType.PRESET
    ),
    SettingMenuItem(
        labelId = R.string.settings_default_home,
        icon = Icons.Rounded.Home,
        lerpFraction = 0.78f,
        actionType = SettingsActionType.DEFAULT_HOME
    ),
    // 정보
    SettingMenuItem(
        labelId = R.string.settings_notice,
        icon = Icons.Rounded.Campaign,
        lerpFraction = 0.89f,
        actionType = SettingsActionType.NOTICE
    ),
    SettingMenuItem(
        labelId = R.string.settings_suggestion,
        icon = Icons.Rounded.Feedback,
        lerpFraction = 1.0f,
        actionType = SettingsActionType.SUGGESTION
    ),
)