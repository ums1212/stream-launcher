package org.comon.streamlauncher.data.remote.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
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

    // .slp 포맷 (schemaVersion=2)
    val slpStorageUrl: String? = null,

    // 소프트 삭제 (isDeleted=true → 마켓에서 숨김, Firebase Functions가 7일 후 실제 삭제)
    // @PropertyName: Kotlin Boolean getter의 'is' 접두사 제거 문제 방지
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,

    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val deletedAt: Long? = null,
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
    hasFeedSettings = hasFeedSettings,
    hasAppDrawerSettings = hasAppDrawerSettings,
    hasWallpaperSettings = hasWallpaperSettings,
    hasThemeSettings = hasThemeSettings,
    slpStorageUrl = slpStorageUrl,
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
    hasFeedSettings = hasFeedSettings,
    hasAppDrawerSettings = hasAppDrawerSettings,
    hasWallpaperSettings = hasWallpaperSettings,
    hasThemeSettings = hasThemeSettings,
    slpStorageUrl = slpStorageUrl,
    isDeleted = false,
)

private fun buildSearchKeywords(name: String, tags: List<String>): List<String> {
    val nameNoSpaces = name.lowercase().replace(" ", "")
    val tagTokens = tags.map { it.lowercase().replace(" ", "") }.filter { it.isNotBlank() }
    val allTokens = (listOf(nameNoSpaces) + tagTokens).filter { it.isNotBlank() }
    return allTokens.flatMap { generateSubstrings(it) }.distinct()
}

private fun generateSubstrings(token: String): List<String> {
    val result = mutableListOf<String>()
    for (start in token.indices) {
        for (end in (start + 1)..token.length) {
            result.add(token.substring(start, end))
        }
    }
    return result
}
