package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface TencentFundFlowApi {
    @GET
    suspend fun getFundFlow(@Url url: String): String
}
