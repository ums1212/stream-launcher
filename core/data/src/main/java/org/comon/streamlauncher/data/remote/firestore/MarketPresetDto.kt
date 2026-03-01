package org.comon.streamlauncher.data.remote.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import java.util.Date

data class MarketPresetDto(
    @DocumentId
    val id: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),
    val schemaVersion: Int = 1,
    val previewImageUrls: List<String> = emptyList(),
    val thumbnailUrl: String = "",
    val downloadCount: Int = 0,
    val likeCount: Int = 0,

    // Home Image Settings
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

    // Feed Settings
    val hasFeedSettings: Boolean = false,
    val useFeed: Boolean = false,
    val youtubeChannelId: String = "",
    val chzzkChannelId: String = "",

    // App Drawer Settings
    val hasAppDrawerSettings: Boolean = false,
    val appDrawerColumns: Int = 4,
    val appDrawerRows: Int = 6,
    val appDrawerIconSizeRatio: Float = 1.0f,

    // Wallpaper Settings
    val hasWallpaperSettings: Boolean = false,
    val wallpaperUrl: String? = null,
    val enableParallax: Boolean = false,

    // Theme Settings
    val hasThemeSettings: Boolean = false,
    val themeColorHex: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
)

fun MarketPresetDto.toDomain(): MarketPreset = MarketPreset(
    id = id,
    authorUid = authorUid,
    authorDisplayName = authorDisplayName,
    name = name,
    description = description,
    tags = tags,
    schemaVersion = schemaVersion,
    previewImageUrls = previewImageUrls,
    thumbnailUrl = thumbnailUrl,
    downloadCount = downloadCount,
    likeCount = likeCount,
    hasTopLeftImage = hasTopLeftImage,
    hasTopRightImage = hasTopRightImage,
    hasBottomLeftImage = hasBottomLeftImage,
    hasBottomRightImage = hasBottomRightImage,
    topLeftIdleUrl = topLeftIdleUrl,
    topLeftExpandedUrl = topLeftExpandedUrl,
    topRightIdleUrl = topRightIdleUrl,
    topRightExpandedUrl = topRightExpandedUrl,
    bottomLeftIdleUrl = bottomLeftIdleUrl,
    bottomLeftExpandedUrl = bottomLeftExpandedUrl,
    bottomRightIdleUrl = bottomRightIdleUrl,
    bottomRightExpandedUrl = bottomRightExpandedUrl,
    hasFeedSettings = hasFeedSettings,
    useFeed = useFeed,
    youtubeChannelId = youtubeChannelId,
    chzzkChannelId = chzzkChannelId,
    hasAppDrawerSettings = hasAppDrawerSettings,
    appDrawerColumns = appDrawerColumns,
    appDrawerRows = appDrawerRows,
    appDrawerIconSizeRatio = appDrawerIconSizeRatio,
    hasWallpaperSettings = hasWallpaperSettings,
    wallpaperUrl = wallpaperUrl,
    enableParallax = enableParallax,
    hasThemeSettings = hasThemeSettings,
    themeColorHex = themeColorHex,
    createdAt = createdAt?.time ?: 0L,
    updatedAt = updatedAt?.time ?: 0L,
)

fun MarketPreset.toDto(): MarketPresetDto = MarketPresetDto(
    id = id,
    authorUid = authorUid,
    authorDisplayName = authorDisplayName,
    name = name,
    description = description,
    tags = tags,
    searchKeywords = buildSearchKeywords(name, tags),
    schemaVersion = schemaVersion,
    previewImageUrls = previewImageUrls,
    thumbnailUrl = thumbnailUrl,
    downloadCount = downloadCount,
    likeCount = likeCount,
    hasTopLeftImage = hasTopLeftImage,
    hasTopRightImage = hasTopRightImage,
    hasBottomLeftImage = hasBottomLeftImage,
    hasBottomRightImage = hasBottomRightImage,
    topLeftIdleUrl = topLeftIdleUrl,
    topLeftExpandedUrl = topLeftExpandedUrl,
    topRightIdleUrl = topRightIdleUrl,
    topRightExpandedUrl = topRightExpandedUrl,
    bottomLeftIdleUrl = bottomLeftIdleUrl,
    bottomLeftExpandedUrl = bottomLeftExpandedUrl,
    bottomRightIdleUrl = bottomRightIdleUrl,
    bottomRightExpandedUrl = bottomRightExpandedUrl,
    hasFeedSettings = hasFeedSettings,
    useFeed = useFeed,
    youtubeChannelId = youtubeChannelId,
    chzzkChannelId = chzzkChannelId,
    hasAppDrawerSettings = hasAppDrawerSettings,
    appDrawerColumns = appDrawerColumns,
    appDrawerRows = appDrawerRows,
    appDrawerIconSizeRatio = appDrawerIconSizeRatio,
    hasWallpaperSettings = hasWallpaperSettings,
    wallpaperUrl = wallpaperUrl,
    enableParallax = enableParallax,
    hasThemeSettings = hasThemeSettings,
    themeColorHex = themeColorHex,
)

private fun buildSearchKeywords(name: String, tags: List<String>): List<String> {
    val nameTokens = name.lowercase().split(" ").filter { it.isNotBlank() }
    val tagTokens = tags.map { it.lowercase() }
    return (nameTokens + tagTokens).distinct()
}
