package com.jiucaihua.app.domain.model

data class PortfolioSnapshot(
    val id: Long = 0,
    val date: String,
    val timestamp: Long,
    val totalMarketValue: Double,
    val totalCost: Double,
    val totalEarnings: Double,
    val totalEarningsPercent: Double,
    val todayEarnings: Double,
    val cash: Double,
    val lossCompensation: Double,
    val categoryValues: Map<String, Double>,
)

enum class ChartRange(val label: String) {
    SEVEN_DAYS("7天"),
    THIRTY_DAYS("30天"),
    NINETY_DAYS("90天"),
    ALL("全部"),
}
