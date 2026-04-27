package com.jiucaihua.app.domain.model

data class PortfolioSummary(
    val baseCurrency: String = "CNY",
    val totalMarketValue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalEarningsPercent: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val holdings: List<Holding> = emptyList(),
    val lastUpdateTime: String = "--",
)
