package com.jiucaihua.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Path

interface HolidayApi {

    @GET("api/holiday/info/{date}")
    suspend fun getHolidayInfo(@Path("date") date: String): String
}
