package com.jiucaihua.app.domain.model

data class PortfolioSummary(
    val baseCurrency: String = "CNY",
    val cash: Double = 0.0,
    val lossCompensation: Double = 0.0,
    val totalMarketValue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalEarningsPercent: Double = 0.0,
    val cumulativeEarnings: Double = 0.0,
    val cumulativeEarningsPercent: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val holdings: List<Holding> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val lastUpdateTime: String = "--",
    val quoteObservations: Map<String, QuoteObservation> = emptyMap(),
)

/** Source time is the provider's display value, never the summary generation time. */
data class QuoteObservation(val sourceTime: String?, val isCached: Boolean, val available: Boolean)
