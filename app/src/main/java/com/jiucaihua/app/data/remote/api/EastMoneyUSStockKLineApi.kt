package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface EastMoneyUSStockKLineApi {

    @GET
    suspend fun getUSStockKLineData(@Url url: String): String
}