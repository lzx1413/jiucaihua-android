package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface SinaStockApi {

    @GET
    suspend fun getStockQuotes(@Url url: String): String
}
