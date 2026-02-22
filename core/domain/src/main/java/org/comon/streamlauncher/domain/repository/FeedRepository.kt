package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.ChannelProfile
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.model.LiveStatus

interface FeedRepository {
    fun getLiveStatus(channelId: String): Flow<Result<LiveStatus>>
    fun getIntegratedFeed(rssUrl: String, youtubeChannelId: String): Flow<Result<List<FeedItem>>>
    fun getChannelProfile(youtubeChannelId: String): Flow<Result<ChannelProfile>>
}
