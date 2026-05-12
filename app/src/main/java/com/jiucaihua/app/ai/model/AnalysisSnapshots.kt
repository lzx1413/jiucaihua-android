package com.jiucaihua.app.ai.model

import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType

data class PortfolioAnalysisSnapshot(
    val generatedAt: String,
    val baseCurrency: String = "CNY",
    val totalInvestmentCny: Double,
    val cashCny: Double,
    val cashPercent: Double,
    val totalMarketValueCny: Double,
    val totalCostCny: Double,
    val totalUnrealizedPnlCny: Double,
    val totalUnrealizedPnlPercent: Double,
    val lossCompensationCny: Double = 0.0,
    val cumulativePnlCny: Double = 0.0,
    val cumulativePnlPercent: Double = 0.0,
    val todayPnlCny: Double,
    val marketSessions: Map<MarketType, MarketSession>,
    val holdings: List<HoldingAnalysisSnapshot>,
    val alertsSummary: AlertsSummary,
    val dataFreshness: DataFreshness,
)

data class HoldingAnalysisSnapshot(
    val code: String,
    val name: String,
    val marketType: MarketType,
    val currency: String,
    val exchangeRate: Double,
    val positionUnits: Double,
    val unitLabel: String,
    val costBasisCny: Double,
    val avgCostPerUnit: Double,
    val currentPrice: Double,
    val marketValueCny: Double,
    val unrealizedPnlCny: Double,
    val unrealizedPnlPercent: Double,
    val latestQuoteTime: String,
    val activeAlerts: List<AlertSnapshot>,
    val relatedNews: List<NewsSnapshot>,
    val dataFreshness: DataFreshness,
)

data class DataFreshness(
    val quoteUpdatedAt: Long?,
    val quoteDisplayTime: String,
    val isQuoteStale: Boolean,
    val source: DataSource,
)

data class AlertsSummary(
    val enabledCount: Int,
    val recentTriggeredCount: Int,
    val affectedCodes: List<String>,
)

data class AlertSnapshot(
    val code: String,
    val name: String,
    val alertType: String,
    val threshold: Double,
    val isEnabled: Boolean,
    val lastTriggeredAt: Long?,
)

data class NewsSnapshot(
    val title: String,
    val summary: String,
    val source: String,
    val time: String,
    val sourceType: String = "",
)

enum class DataSource {
    LIVE,
    CACHE,
}
