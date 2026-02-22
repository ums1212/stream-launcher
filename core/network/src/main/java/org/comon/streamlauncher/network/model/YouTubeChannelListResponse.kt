package org.comon.streamlauncher.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeChannelListResponse(
    @SerialName("items") val items: List<YouTubeChannelItem> = emptyList(),
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeChannelItem(
    @SerialName("id") val id: String = "",
)
