package com.jiucaihua.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KLineResponseDto(
    @Json(name = "data") val data: KLineDataDto?
)

@JsonClass(generateAdapter = true)
data class KLineDataDto(
    @Json(name = "code") val code: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "klines") val klines: List<String>?,
)
