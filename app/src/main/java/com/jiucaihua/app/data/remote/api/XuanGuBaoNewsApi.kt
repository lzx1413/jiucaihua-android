package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface XuanGuBaoNewsApi {

    @GET("api/v6/message/newsflash")
    suspend fun getNewsFlash(
        @Query("limit") limit: Int,
        @Query("subj_ids") subjectIds: String,
        @Query("platform") platform: String = "pcweb",
    ): String
}
