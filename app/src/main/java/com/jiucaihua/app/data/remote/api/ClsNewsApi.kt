package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Url

interface ClsNewsApi {

    @GET
    suspend fun getTelegraphList(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
    ): String
}
