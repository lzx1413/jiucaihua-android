package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TencentSecurityEventApi {
    @GET("ifzqgtimg/appstock/news/info/search")
    suspend fun getNews(
        @Query("symbol") symbol: String,
        @Query("type") type: Int,
        @Query("page") page: Int,
    ): String

    @GET("ifzqgtimg/appstock/news/noticeList/search")
    suspend fun getNotices(
        @Query("symbol") symbol: String,
        @Query("noticeType") noticeType: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): String

    @GET("ifzqgtimg/appstock/app/investRate/getReport")
    suspend fun getResearch(
        @Query("symbol") symbol: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): String
}
