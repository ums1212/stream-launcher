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
    @SerialName("snippet") val snippet: YouTubeChannelSnippet? = null,
    @SerialName("statistics") val statistics: YouTubeChannelStatistics? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeChannelSnippet(
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("thumbnails") val thumbnails: YouTubeThumbnails = YouTubeThumbnails(),
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeChannelStatistics(
    @SerialName("subscriberCount") val subscriberCount: String = "0",
    @SerialName("videoCount") val videoCount: String = "0",
    @SerialName("viewCount") val viewCount: String = "0",
)
