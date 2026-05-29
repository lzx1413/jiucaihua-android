package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.AlertSnapshot
import com.jiucaihua.app.ai.model.AlertsSummary
import com.jiucaihua.app.ai.model.DataFreshness
import com.jiucaihua.app.ai.model.DataSource
import com.jiucaihua.app.ai.model.HoldingAnalysisSnapshot
import com.jiucaihua.app.ai.model.NewsSnapshot
import com.jiucaihua.app.ai.model.PortfolioAnalysisSnapshot
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class BuildPortfolioAnalysisSnapshotUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val alertRepository: AlertRepository,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
) {
    suspend operator fun invoke(): PortfolioAnalysisSnapshot {
        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        val alerts = alertRepository.getEnabledAlerts()
        val marketSessions = isMarketOpenUseCase.getMarketSessions()
        val holdings = summary.holdings.map { holding ->
            holding.toAnalysisSnapshot(
                activeAlerts = alerts.filter { it.code == holding.code },
                relatedNews = emptyList(),
                quoteDisplayTime = summary.lastUpdateTime,
                quoteUpdatedAt = null,
                source = DataSource.LIVE,
            )
        }
        return PortfolioAnalysisSnapshot(
            generatedAt = timestampFormatter.format(Date()),
            totalInvestmentCny = summary.totalInvestment,
            cashCny = summary.cash,
            cashPercent = if (summary.totalInvestment > 0) summary.cash / summary.totalInvestment * 100 else 0.0,
            totalMarketValueCny = summary.totalMarketValue,
            totalCostCny = summary.totalCost,
            totalUnrealizedPnlCny = summary.totalEarnings,
            totalUnrealizedPnlPercent = summary.totalEarningsPercent,
            lossCompensationCny = summary.lossCompensation,
            cumulativePnlCny = summary.cumulativeEarnings,
            cumulativePnlPercent = summary.cumulativeEarningsPercent,
            todayPnlCny = summary.todayEarnings,
            marketSessions = marketSessions,
            holdings = holdings,
            alertsSummary = alerts.toSummary(),
            dataFreshness = DataFreshness(
                quoteUpdatedAt = null,
                quoteDisplayTime = summary.lastUpdateTime,
                isQuoteStale = summary.lastUpdateTime == "--",
                source = DataSource.LIVE,
            ),
        )
    }

    private fun Holding.toAnalysisSnapshot(
        activeAlerts: List<PriceAlert>,
        relatedNews: List<StockArticle>,
        quoteDisplayTime: String,
        quoteUpdatedAt: Long?,
        source: DataSource,
    ): HoldingAnalysisSnapshot {
        return HoldingAnalysisSnapshot(
            code = code,
            name = name,
            marketType = marketType,
            currency = currency,
            exchangeRate = exchangeRate,
            positionUnits = holdingShares,
            unitLabel = if (marketType == MarketType.FUND) "份" else "股",
            costBasisCny = calcCostBasisCny(this),
            avgCostPerUnit = costPrice,
            currentPrice = currentPrice,
            marketValueCny = marketValueCNY,
            unrealizedPnlCny = earningsCNY,
            unrealizedPnlPercent = earningsPercent,
            latestQuoteTime = quoteDisplayTime,
            activeAlerts = activeAlerts.map { it.toSnapshot() },
            relatedNews = relatedNews.map { it.toSnapshot() },
            dataFreshness = DataFreshness(
                quoteUpdatedAt = quoteUpdatedAt,
                quoteDisplayTime = quoteDisplayTime,
                isQuoteStale = quoteDisplayTime == "--",
                source = source,
            ),
        )
    }

    private fun calcCostBasisCny(holding: Holding): Double {
        return if (holding.marketType == MarketType.FUND) {
            holding.holdingAmount * holding.exchangeRate
        } else {
            holding.costPrice * holding.holdingShares * holding.exchangeRate
        }
    }

    private fun PriceAlert.toSnapshot(): AlertSnapshot {
        return AlertSnapshot(
            code = code,
            name = name,
            alertType = alertType.name,
            threshold = threshold,
            actionHint = actionHint,
            isEnabled = isEnabled,
            lastTriggeredAt = lastTriggeredAt,
        )
    }

    private fun StockArticle.toSnapshot(): NewsSnapshot {
        return NewsSnapshot(
            title = title,
            summary = summary,
            source = source,
            time = time,
            sourceType = sourceType.displayName,
        )
    }

    private fun List<PriceAlert>.toSummary(): AlertsSummary {
        val now = System.currentTimeMillis()
        val recentWindowMs = 24 * 60 * 60 * 1000L
        return AlertsSummary(
            enabledCount = size,
            recentTriggeredCount = count { alert ->
                val lastTriggeredAt = alert.lastTriggeredAt ?: return@count false
                now - lastTriggeredAt <= recentWindowMs
            },
            affectedCodes = map { it.code }.distinct().sorted(),
        )
    }

    companion object {
        private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
}

class BuildHoldingAnalysisSnapshotUseCase @Inject constructor(
    private val holdingRepository: HoldingRepository,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val alertRepository: AlertRepository,
    private val newsRepository: NewsRepository,
) {
    suspend operator fun invoke(code: String): HoldingAnalysisSnapshot? {
        val holding = holdingRepository.getHoldingByCode(code)
        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        val matchedHolding = summary.holdings.firstOrNull { it.code == code } ?: holding ?: return null
        val activeAlerts = alertRepository.getEnabledAlerts().filter { it.code == code }
        val relatedNews = newsRepository.getStockNews(matchedHolding.name, limit = 5)
        return matchedHolding.toAnalysisSnapshot(
            activeAlerts = activeAlerts,
            relatedNews = relatedNews,
            quoteDisplayTime = summary.lastUpdateTime,
            quoteUpdatedAt = null,
            source = DataSource.LIVE,
        )
    }

    private fun Holding.toAnalysisSnapshot(
        activeAlerts: List<PriceAlert>,
        relatedNews: List<StockArticle>,
        quoteDisplayTime: String,
        quoteUpdatedAt: Long?,
        source: DataSource,
    ): HoldingAnalysisSnapshot {
        return HoldingAnalysisSnapshot(
            code = code,
            name = name,
            marketType = marketType,
            currency = currency,
            exchangeRate = exchangeRate,
            positionUnits = holdingShares,
            unitLabel = if (marketType == MarketType.FUND) "份" else "股",
            costBasisCny = if (marketType == MarketType.FUND) holdingAmount * exchangeRate else costPrice * holdingShares * exchangeRate,
            avgCostPerUnit = costPrice,
            currentPrice = currentPrice,
            marketValueCny = marketValueCNY,
            unrealizedPnlCny = earningsCNY,
            unrealizedPnlPercent = earningsPercent,
            latestQuoteTime = quoteDisplayTime,
            activeAlerts = activeAlerts.map {
                AlertSnapshot(
                    code = it.code,
                    name = it.name,
                    alertType = it.alertType.name,
                    threshold = it.threshold,
                    actionHint = it.actionHint,
                    isEnabled = it.isEnabled,
                    lastTriggeredAt = it.lastTriggeredAt,
                )
            },
            relatedNews = relatedNews.map {
                NewsSnapshot(
                    title = it.title,
                    summary = it.summary,
                    source = it.source,
                    time = it.time,
                    sourceType = it.sourceType.displayName,
                )
            },
            dataFreshness = DataFreshness(
                quoteUpdatedAt = quoteUpdatedAt,
                quoteDisplayTime = quoteDisplayTime,
                isQuoteStale = quoteDisplayTime == "--",
                source = source,
            ),
        )
    }
}
