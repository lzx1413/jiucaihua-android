package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TencentSearchApi {

    @GET("ifzqgtimg/appstock/smartbox/search/get")
    suspend fun search(
        @Query("q") keyword: String,
    ): String
}
