package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface EastMoneyNewsApi {

    @GET
    suspend fun getKuaixun(
        @Url url: String,
    ): String
}
