package org.comon.streamlauncher.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@OptIn(InternalSerializationApi::class)
@Serializable
@XmlSerialName("rss", "", "")
data class RssFeedResponse(
    @XmlSerialName("channel", "", "")
    val channel: RssChannel
)

@OptIn(InternalSerializationApi::class)
@Serializable
@XmlSerialName("channel", "", "")
data class RssChannel(
    val title: String = "",
    val link: String = "",
    val description: String = "",
    @XmlSerialName("item", "", "")
    val items: List<RssItem> = emptyList()
)

@OptIn(InternalSerializationApi::class)
@Serializable
@XmlSerialName("item", "", "")
data class RssItem(
    val title: String = "",
    val link: String = "",
    val description: String = "",
    val pubDate: String = ""
)
