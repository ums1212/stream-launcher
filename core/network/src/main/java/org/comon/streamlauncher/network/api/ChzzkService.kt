package org.comon.streamlauncher.network.api

import org.comon.streamlauncher.network.model.ChzzkLiveResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface ChzzkService {
    @GET
    suspend fun getLiveDetail(@Url url: String): ChzzkLiveResponse
}
