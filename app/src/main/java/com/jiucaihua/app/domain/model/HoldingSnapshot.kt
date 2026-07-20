package com.jiucaihua.app.domain.model

data class HoldingSnapshot(
    val id: Long = 0,
    val code: String,
    val name: String,
    val marketType: MarketType,
    val date: String,
    val timestamp: Long,
    val holdingShares: Double,
    val currentPrice: Double,
    val costPrice: Double,
    val exchangeRate: Double,
    val marketValueCny: Double,
    val costCny: Double,
    val dailyEarningsCny: Double,
)
