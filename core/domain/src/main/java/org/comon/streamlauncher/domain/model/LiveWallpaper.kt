package org.comon.streamlauncher.domain.model

data class LiveWallpaper(
    val id: Int = 0,
    val name: String,
    val fileUri: String,
    val thumbnailUri: String?,
    val createdAt: Long = System.currentTimeMillis(),
)
