package org.comon.streamlauncher.data.slp

import kotlinx.serialization.Serializable

/** .slp 아카이브 내 manifest.json 스키마 */
@Serializable
data class SlpManifest(
    val formatVersion: Int = 1,
    val name: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val images: SlpImagePaths = SlpImagePaths(),
    val previews: List<String> = emptyList(),
    val cellFlags: SlpCellFlags = SlpCellFlags(),
    val feedSettings: SlpFeedSettings? = null,
    val appDrawerSettings: SlpAppDrawerSettings? = null,
    val wallpaperSettings: SlpWallpaperSettings? = null,
    val themeSettings: SlpThemeSettings? = null,
)

/** ZIP 내 이미지 상대 경로 (null = 해당 슬롯 없음) */
@Serializable
data class SlpImagePaths(
    val topLeftIdle: String? = null,
    val topLeftExpanded: String? = null,
    val topRightIdle: String? = null,
    val topRightExpanded: String? = null,
    val bottomLeftIdle: String? = null,
    val bottomLeftExpanded: String? = null,
    val bottomRightIdle: String? = null,
    val bottomRightExpanded: String? = null,
    val wallpaper: String? = null,
)

@Serializable
data class SlpCellFlags(
    val hasTopLeft: Boolean = false,
    val hasTopRight: Boolean = false,
    val hasBottomLeft: Boolean = false,
    val hasBottomRight: Boolean = false,
)

@Serializable
data class SlpFeedSettings(
    val enabled: Boolean = false,
    val useFeed: Boolean = false,
    val youtubeChannelId: String = "",
    val chzzkChannelId: String = "",
)

@Serializable
data class SlpAppDrawerSettings(
    val enabled: Boolean = false,
    val columns: Int = 4,
    val rows: Int = 6,
    val iconSizeRatio: Float = 1.0f,
)

@Serializable
data class SlpWallpaperSettings(
    val enabled: Boolean = false,
    val enableParallax: Boolean = false,
)

@Serializable
data class SlpThemeSettings(
    val enabled: Boolean = false,
    val colorHex: String? = null,
)
