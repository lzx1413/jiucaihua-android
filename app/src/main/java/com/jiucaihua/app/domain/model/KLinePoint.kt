package com.jiucaihua.app.domain.model

data class KLinePoint(
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val amount: Double = 0.0,
    val changePercent: Double = 0.0,
)
