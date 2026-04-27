package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TencentKLineApi {

    @GET("appstock/app/fqkline/get")
    suspend fun getKLineData(
        @Query("param") param: String,
    ): String
}
