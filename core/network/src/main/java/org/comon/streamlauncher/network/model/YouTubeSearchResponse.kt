package org.comon.streamlauncher.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeSearchResponse(
    @SerialName("items") val items: List<YouTubeSearchItem> = emptyList(),
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeSearchItem(
    @SerialName("id") val id: YouTubeVideoId = YouTubeVideoId(),
    @SerialName("snippet") val snippet: YouTubeSnippet = YouTubeSnippet(),
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeVideoId(
    @SerialName("videoId") val videoId: String = "",
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeSnippet(
    @SerialName("title") val title: String = "",
    @SerialName("publishedAt") val publishedAt: String = "",
    @SerialName("thumbnails") val thumbnails: YouTubeThumbnails = YouTubeThumbnails(),
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeThumbnails(
    @SerialName("default") val default: YouTubeThumbnail? = null,
    @SerialName("high") val high: YouTubeThumbnail? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class YouTubeThumbnail(
    @SerialName("url") val url: String = "",
)
