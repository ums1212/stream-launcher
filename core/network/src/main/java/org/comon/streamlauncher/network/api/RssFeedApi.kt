package org.comon.streamlauncher.network.api

import org.comon.streamlauncher.network.model.RssFeedResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface RssFeedApi {
    @GET
    suspend fun getRssFeed(@Url url: String): RssFeedResponse
}
