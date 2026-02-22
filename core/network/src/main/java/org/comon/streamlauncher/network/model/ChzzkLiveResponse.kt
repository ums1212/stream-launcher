package org.comon.streamlauncher.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class ChzzkLiveResponse(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("content") val content: ChzzkLiveContent? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ChzzkLiveContent(
    @SerialName("status") val status: String = "",
    @SerialName("liveTitle") val liveTitle: String = "",
    @SerialName("concurrentUserCount") val concurrentUserCount: Int = 0,
    @SerialName("liveImageUrl") val liveImageUrl: String = "",
)
