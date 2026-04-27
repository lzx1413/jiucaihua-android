package com.jiucaihua.app.ai.provider

import retrofit2.http.GET
import retrofit2.http.Header

interface DeepSeekApi {
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String,
    ): DeepSeekModelsResponse
}
