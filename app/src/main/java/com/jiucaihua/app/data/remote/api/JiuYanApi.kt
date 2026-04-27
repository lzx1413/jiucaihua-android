package com.jiucaihua.app.data.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

interface JiuYanApi {

    @GET
    suspend fun getTokenSource(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
    ): Response<String>

    @POST("jystock-app/api/v2/article/search")
    suspend fun searchArticles(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): String
}
