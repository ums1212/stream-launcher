package org.comon.streamlauncher.domain.model.preset

data class MarketPreset(
    val id: String = "",
    val authorUid: String = "",
    val authorDisplayName: String = "",
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val schemaVersion: Int = 2,
    val previewImageUrls: List<String> = emptyList(),
    val thumbnailUrl: String = "",
    val downloadCount: Int = 0,
    val likeCount: Int = 0,

    // Home Image flags (UI 칩 표시 + applySettings 결정)
    val hasTopLeftImage: Boolean = false,
    val hasTopRightImage: Boolean = false,
    val hasBottomLeftImage: Boolean = false,
    val hasBottomRightImage: Boolean = false,

    // Settings flags (UI 칩 표시 + applySettings 결정)
    val hasFeedSettings: Boolean = false,
    val hasAppDrawerSettings: Boolean = false,
    val hasWallpaperSettings: Boolean = false,
    val hasThemeSettings: Boolean = false,

    // .slp 포맷 (schemaVersion=2) — Storage에 업로드된 단일 압축 파일 URL
    val slpStorageUrl: String? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
