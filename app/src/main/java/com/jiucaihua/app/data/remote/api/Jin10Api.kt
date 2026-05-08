package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface Jin10Api {

    @GET("get_flash_list")
    suspend fun getFlashList(
        @Query("channel") channel: String = "-8200",
        @Query("vip") vip: Int = 1,
        @HeaderMap headers: Map<String, String>,
    ): String
}
