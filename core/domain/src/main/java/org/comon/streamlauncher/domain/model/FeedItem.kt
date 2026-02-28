package org.comon.streamlauncher.domain.model

sealed interface FeedItem {
    val title: String
    val dateMillis: Long

    data class VideoItem(
        override val title: String,
        override val dateMillis: Long,
        val thumbnailUrl: String,
        val videoLink: String,
    ) : FeedItem
}
