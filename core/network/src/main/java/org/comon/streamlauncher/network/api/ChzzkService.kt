package org.comon.streamlauncher.network.api

import org.comon.streamlauncher.network.model.ChzzkLiveResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ChzzkService {
    @GET("service/v3/channels/{channelId}/live-detail")
    suspend fun getLiveDetail(@Path("channelId") channelId: String): ChzzkLiveResponse
}
