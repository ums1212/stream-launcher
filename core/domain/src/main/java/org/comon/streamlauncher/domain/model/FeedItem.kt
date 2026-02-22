package org.comon.streamlauncher.domain.model

sealed interface FeedItem {
    val title: String
    val dateMillis: Long

    data class NoticeItem(
        override val title: String,
        override val dateMillis: Long,
        val link: String,
        val source: String,
    ) : FeedItem

    data class VideoItem(
        override val title: String,
        override val dateMillis: Long,
        val thumbnailUrl: String,
        val videoLink: String,
    ) : FeedItem
}
