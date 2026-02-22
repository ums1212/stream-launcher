package org.comon.streamlauncher.domain.model

data class ChannelProfile(
    val channelId: String,
    val name: String,
    val avatarUrl: String,
    val subscriberCount: Long,
    val videoCount: Long,
)
