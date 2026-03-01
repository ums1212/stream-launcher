package org.comon.streamlauncher.data.local.room.preset

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.comon.streamlauncher.domain.model.preset.Preset

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    
    // Home Image
    val hasTopLeftImage: Boolean,
    val hasTopRightImage: Boolean,
    val hasBottomLeftImage: Boolean,
    val hasBottomRightImage: Boolean,
    val topLeftIdleUri: String?,
    val topLeftExpandedUri: String?,
    val topRightIdleUri: String?,
    val topRightExpandedUri: String?,
    val bottomLeftIdleUri: String?,
    val bottomLeftExpandedUri: String?,
    val bottomRightIdleUri: String?,
    val bottomRightExpandedUri: String?,
    
    // Feed Settings
    val hasFeedSettings: Boolean,
    val useFeed: Boolean,
    val youtubeChannelId: String,
    val chzzkChannelId: String,
    
    // App Drawer
    val hasAppDrawerSettings: Boolean,
    val appDrawerColumns: Int,
    val appDrawerRows: Int,
    val appDrawerIconSizeRatio: Float,
    
    // Wallpaper
    val hasWallpaperSettings: Boolean,
    val wallpaperUri: String?,
    val enableParallax: Boolean,
    
    // Theme Color
    val hasThemeSettings: Boolean,
    val themeColorHex: String?,
    
    val createdAt: Long
)

fun PresetEntity.toDomain() = Preset(
    id = id,
    name = name,
    hasTopLeftImage = hasTopLeftImage,
    hasTopRightImage = hasTopRightImage,
    hasBottomLeftImage = hasBottomLeftImage,
    hasBottomRightImage = hasBottomRightImage,
    topLeftIdleUri = topLeftIdleUri,
    topLeftExpandedUri = topLeftExpandedUri,
    topRightIdleUri = topRightIdleUri,
    topRightExpandedUri = topRightExpandedUri,
    bottomLeftIdleUri = bottomLeftIdleUri,
    bottomLeftExpandedUri = bottomLeftExpandedUri,
    bottomRightIdleUri = bottomRightIdleUri,
    bottomRightExpandedUri = bottomRightExpandedUri,
    hasFeedSettings = hasFeedSettings,
    useFeed = useFeed,
    youtubeChannelId = youtubeChannelId,
    chzzkChannelId = chzzkChannelId,
    hasAppDrawerSettings = hasAppDrawerSettings,
    appDrawerColumns = appDrawerColumns,
    appDrawerRows = appDrawerRows,
    appDrawerIconSizeRatio = appDrawerIconSizeRatio,
    hasWallpaperSettings = hasWallpaperSettings,
    wallpaperUri = wallpaperUri,
    enableParallax = enableParallax,
    hasThemeSettings = hasThemeSettings,
    themeColorHex = themeColorHex,
    createdAt = createdAt
)

fun Preset.toEntity() = PresetEntity(
    id = id,
    name = name,
    hasTopLeftImage = hasTopLeftImage,
    hasTopRightImage = hasTopRightImage,
    hasBottomLeftImage = hasBottomLeftImage,
    hasBottomRightImage = hasBottomRightImage,
    topLeftIdleUri = topLeftIdleUri,
    topLeftExpandedUri = topLeftExpandedUri,
    topRightIdleUri = topRightIdleUri,
    topRightExpandedUri = topRightExpandedUri,
    bottomLeftIdleUri = bottomLeftIdleUri,
    bottomLeftExpandedUri = bottomLeftExpandedUri,
    bottomRightIdleUri = bottomRightIdleUri,
    bottomRightExpandedUri = bottomRightExpandedUri,
    hasFeedSettings = hasFeedSettings,
    useFeed = useFeed,
    youtubeChannelId = youtubeChannelId,
    chzzkChannelId = chzzkChannelId,
    hasAppDrawerSettings = hasAppDrawerSettings,
    appDrawerColumns = appDrawerColumns,
    appDrawerRows = appDrawerRows,
    appDrawerIconSizeRatio = appDrawerIconSizeRatio,
    hasWallpaperSettings = hasWallpaperSettings,
    wallpaperUri = wallpaperUri,
    enableParallax = enableParallax,
    hasThemeSettings = hasThemeSettings,
    themeColorHex = themeColorHex,
    createdAt = createdAt
)
