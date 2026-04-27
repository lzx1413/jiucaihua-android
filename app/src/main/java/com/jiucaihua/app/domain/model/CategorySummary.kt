package com.jiucaihua.app.domain.model

data class CategorySummary(
    val marketType: MarketType,
    val totalMarketValue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalEarningsPercent: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val holdings: List<Holding> = emptyList(),
) {
    val label: String = marketType.label
}