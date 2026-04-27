package com.jiucaihua.app.domain.model

data class StockQuote(
    val code: String,
    val name: String,
    val price: Double,
    val yestClose: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val amount: Double,
    val changePercent: Double,
    val changeAmount: Double,
    val time: String,
    val marketType: MarketType,
)
