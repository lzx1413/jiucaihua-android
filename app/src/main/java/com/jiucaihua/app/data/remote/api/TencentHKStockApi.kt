package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface TencentHKStockApi {

    @GET
    suspend fun getHKStockQuotes(@Url url: String): String
}
