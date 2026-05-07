package com.jiucaihua.app.domain.model

data class WatchlistItem(
    val id: Long = 0,
    val code: String,
    val name: String,
    val marketType: MarketType,
    val currentPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val changeAmount: Double = 0.0,
)
