package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface EastMoneyFundFlowApi {

    @GET
    suspend fun getFundFlowData(@Url url: String): String
}