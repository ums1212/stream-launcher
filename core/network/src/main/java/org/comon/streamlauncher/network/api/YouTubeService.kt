package org.comon.streamlauncher.network.api

import org.comon.streamlauncher.network.model.YouTubeChannelListResponse
import org.comon.streamlauncher.network.model.YouTubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface YouTubeService {
    @GET
    suspend fun searchVideos(
        @Url url: String,
        @Query("part") part: String,
        @Query("channelId") channelId: String,
        @Query("order") order: String,
        @Query("type") type: String,
        @Query("eventType") eventType: String? = null,
        @Query("maxResults") maxResults: Int,
        @Query("key") apiKey: String,
    ): YouTubeSearchResponse

    @GET
    suspend fun getChannelByHandle(
        @Url url: String,
        @Query("part") part: String,
        @Query("forHandle") handle: String,
        @Query("key") apiKey: String,
    ): YouTubeChannelListResponse

    @GET
    suspend fun getChannelInfo(
        @Url url: String,
        @Query("part") part: String,
        @Query("id") id: String? = null,
        @Query("forHandle") handle: String? = null,
        @Query("key") apiKey: String,
    ): YouTubeChannelListResponse
}
