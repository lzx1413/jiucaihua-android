package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TencentSecurityInsightApi {
    @GET("ifzqgtimg/stock/relate/data/plate")
    suspend fun getPlates(@Query("code") code: String): String

    @GET("ifzqgtimg/stock/relate/data/relate")
    suspend fun getRelatedSecurities(@Query("code") code: String): String

    @GET("ifzqgtimg/appstock/hs/ltgd/get")
    suspend fun getShareholders(
        @Query("code") code: String,
        @Query("type") type: String = "ltgd",
    ): String
}
