package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WallstreetCnApi {

    @GET("apiv1/content/lives")
    suspend fun getLives(
        @Query("channel") channel: String = "global-channel",
        @Query("client") client: String = "pc",
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String = "0",
    ): String
}
