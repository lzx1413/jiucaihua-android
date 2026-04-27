package com.jiucaihua.app.ai.provider

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiCompatibleChatApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiCompatibleChatRequest,
    ): Response<OpenAiCompatibleChatResponse>
}
