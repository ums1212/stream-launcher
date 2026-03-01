package org.comon.streamlauncher.domain.model.preset

data class MarketPreset(
    val id: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val schemaVersion: Int = 1,
    val previewImageUrls: List<String> = emptyList(),
    val thumbnailUrl: String = "",
    val downloadCount: Int = 0,
    val likeCount: Int = 0,

    // 1. Home Image Settings (Storage URL 기반)
    val hasTopLeftImage: Boolean = false,
    val hasTopRightImage: Boolean = false,
    val hasBottomLeftImage: Boolean = false,
    val hasBottomRightImage: Boolean = false,
    val topLeftIdleUrl: String? = null,
    val topLeftExpandedUrl: String? = null,
    val topRightIdleUrl: String? = null,
    val topRightExpandedUrl: String? = null,
    val bottomLeftIdleUrl: String? = null,
    val bottomLeftExpandedUrl: String? = null,
    val bottomRightIdleUrl: String? = null,
    val bottomRightExpandedUrl: String? = null,

    // 2. Feed Settings
    val hasFeedSettings: Boolean = false,
    val useFeed: Boolean = false,
    val youtubeChannelId: String = "",
    val chzzkChannelId: String = "",

    // 3. App Drawer Settings
    val hasAppDrawerSettings: Boolean = false,
    val appDrawerColumns: Int = 4,
    val appDrawerRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,

    // 4. Wallpaper Settings (Storage URL 기반)
    val hasWallpaperSettings: Boolean = false,
    val wallpaperUrl: String? = null,
    val enableParallax: Boolean = false,

    // 5. Theme Color Settings
    val hasThemeSettings: Boolean = false,
    val themeColorHex: String? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
