package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

interface FundApi {

    @GET
    suspend fun getFundEstimate(@Url url: String): String
}
