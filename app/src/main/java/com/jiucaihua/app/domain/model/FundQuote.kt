package com.jiucaihua.app.domain.model

data class FundQuote(
    val code: String,
    val name: String,
    val estimatedValue: Double,
    val dailyChangePercent: Double,
    val netAssetValue: Double,
    val estimateTime: String,
    val navDate: String,
)
