package org.comon.streamlauncher.domain.model

data class LiveStatus(
    val isLive: Boolean,
    val title: String,
    val viewerCount: Int,
    val thumbnailUrl: String,
    val channelId: String,
)
