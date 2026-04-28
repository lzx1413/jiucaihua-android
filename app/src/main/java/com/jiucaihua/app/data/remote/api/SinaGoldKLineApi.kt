package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface SinaGoldKLineApi {

    @GET
    suspend fun getGoldKLine(@Url url: String): String
}
