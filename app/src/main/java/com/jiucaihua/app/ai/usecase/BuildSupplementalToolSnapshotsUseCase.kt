package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.AlertSnapshot
import com.jiucaihua.app.ai.model.AlertsToolSnapshot
import com.jiucaihua.app.ai.model.KLinePointSnapshot
import com.jiucaihua.app.ai.model.KLineToolSnapshot
import com.jiucaihua.app.ai.model.MarketNewsDigest
import com.jiucaihua.app.ai.model.NewsSnapshot
import com.jiucaihua.app.ai.model.WhatIfAnalysisSnapshot
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NewsTopic
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class BuildKLineToolSnapshotUseCase @Inject constructor(
    private val getKLineDataUseCase: GetKLineDataUseCase,
    private val fundRepository: FundRepository,
) {
    suspend operator fun invoke(
        code: String,
        period: KLinePeriod,
        limit: Int,
    ): KLineToolSnapshot {
        val data = if (MarketType.fromCode(code) == MarketType.FUND) {
            fundRepository.getFundNavHistory(code, limit)
        } else {
            getKLineDataUseCase(code, period, limit)
        }
        return data.toSnapshot()
    }

    private fun KLineData.toSnapshot(): KLineToolSnapshot {
        val latestPoint = points.lastOrNull()
        val highs = points.map { it.high }
        val lows = points.map { it.low }
        return KLineToolSnapshot(
            code = code,
            name = name,
            period = period,
            pointsCount = points.size,
            latestPoint = latestPoint?.toSnapshot(),
            highestHigh = highs.maxOrNull() ?: 0.0,
            lowestLow = lows.minOrNull() ?: 0.0,
            points = points.map { it.toSnapshot() },
        )
    }

    private fun com.jiucaihua.app.domain.model.KLinePoint.toSnapshot(): KLinePointSnapshot {
        return KLinePointSnapshot(
            date = date,
            open = open,
            close = close,
            high = high,
            low = low,
            volume = volume,
            changePercent = changePercent,
        )
    }
}

class BuildMarketNewsDigestUseCase @Inject constructor(
    private val newsRepository: NewsRepository,
) {
    suspend operator fun invoke(limit: Int, topic: NewsTopic? = null, query: String? = null): MarketNewsDigest {
        val newsList = if (!query.isNullOrBlank()) {
            newsRepository.searchNews(query, topic, limit)
        } else if (topic != null) {
            newsRepository.getMarketNews(topic, limit)
        } else {
            newsRepository.getMarketNews(limit)
        }
        val items = newsList.map {
            NewsSnapshot(
                title = it.title,
                summary = it.summary,
                source = it.source,
                time = it.time,
                sourceType = it.sourceType.displayName,
            )
        }
        return MarketNewsDigest(
            generatedAt = timestampFormatter.format(Date()),
            total = items.size,
            items = items,
        )
    }
}

class BuildAlertsToolSnapshotUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    suspend operator fun invoke(code: String?): AlertsToolSnapshot {
        val alerts = alertRepository.getAllAlerts().first()
            .filter { code == null || it.code == code }
        val now = System.currentTimeMillis()
        val recentWindowMs = 24 * 60 * 60 * 1000L
        return AlertsToolSnapshot(
            total = alerts.size,
            enabledCount = alerts.count { it.isEnabled },
            recentTriggeredCount = alerts.count { alert ->
                val lastTriggeredAt = alert.lastTriggeredAt ?: return@count false
                now - lastTriggeredAt <= recentWindowMs
            },
            alerts = alerts.map { it.toSnapshot() },
        )
    }

    private fun PriceAlert.toSnapshot(): AlertSnapshot {
        return AlertSnapshot(
            code = code,
            name = name,
            alertType = alertType.name,
            threshold = threshold,
            isEnabled = isEnabled,
            lastTriggeredAt = lastTriggeredAt,
        )
    }
}

class BuildWhatIfAnalysisSnapshotUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
) {
    suspend operator fun invoke(
        code: String,
        targetPrice: Double?,
        changePercent: Double?,
    ): WhatIfAnalysisSnapshot {
        require((targetPrice != null) xor (changePercent != null)) {
            "Provide exactly one of targetPrice or changePercent"
        }

        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        val holding = summary.holdings.firstOrNull { it.code == code }
            ?: error("Holding not found: $code")
        val currentPrice = holding.currentPrice
        require(currentPrice > 0) { "Current price unavailable for $code" }

        val finalTargetPrice = targetPrice ?: currentPrice * (1 + (changePercent ?: 0.0) / 100)
        require(finalTargetPrice > 0) { "Target price must be positive" }

        val costBasisCny = calcCostBasisCny(holding)
        val targetMarketValueCny = finalTargetPrice * holding.holdingShares * holding.exchangeRate
        val targetUnrealizedPnlCny = targetMarketValueCny - costBasisCny
        val targetUnrealizedPnlPercent = if (costBasisCny > 0) {
            targetUnrealizedPnlCny / costBasisCny * 100
        } else {
            0.0
        }

        return WhatIfAnalysisSnapshot(
            code = holding.code,
            name = holding.name,
            positionUnits = holding.holdingShares,
            unitLabel = if (holding.marketType == MarketType.FUND) "份" else "股",
            currentPrice = currentPrice,
            targetPrice = finalTargetPrice,
            targetChangePercent = (finalTargetPrice - currentPrice) / currentPrice * 100,
            costBasisCny = costBasisCny,
            currentMarketValueCny = holding.marketValueCNY,
            targetMarketValueCny = targetMarketValueCny,
            currentUnrealizedPnlCny = holding.earningsCNY,
            targetUnrealizedPnlCny = targetUnrealizedPnlCny,
            pnlDifferenceCny = targetUnrealizedPnlCny - holding.earningsCNY,
            currentUnrealizedPnlPercent = holding.earningsPercent,
            targetUnrealizedPnlPercent = targetUnrealizedPnlPercent,
        )
    }

    private fun calcCostBasisCny(holding: Holding): Double {
        return if (holding.marketType == MarketType.FUND) {
            holding.holdingAmount * holding.exchangeRate
        } else {
            holding.costPrice * holding.holdingShares * holding.exchangeRate
        }
    }
}

private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
