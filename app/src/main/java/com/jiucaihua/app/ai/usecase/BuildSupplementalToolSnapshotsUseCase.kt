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
import com.jiucaihua.app.domain.util.TechnicalIndicators
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class BuildKLineToolSnapshotUseCase @Inject constructor(
    private val getKLineDataUseCase: GetKLineDataUseCase,
    private val fundRepository: FundRepository,
) {
    suspend operator fun invoke(
        code: String,
        period: KLinePeriod,
        limit: Int,
        includeLatest: Boolean = false,
        includeIndicators: Boolean = true,
    ): KLineToolSnapshot {
        val historyLimit = if (includeIndicators) limit + 119 else limit
        val data = if (MarketType.fromCode(code) == MarketType.FUND) {
            fundRepository.getFundNavHistory(code, historyLimit)
        } else {
            getKLineDataUseCase(code, period, historyLimit)
        }
        return data.toSnapshot(limit, includeLatest, includeIndicators)
    }

    private fun KLineData.toSnapshot(limit: Int, includeLatest: Boolean, includeIndicators: Boolean): KLineToolSnapshot {
        val returnedPoints = points.takeLast(limit)
        val highs = returnedPoints.map { it.high }
        val lows = returnedPoints.map { it.low }
        val ma5 = TechnicalIndicators.calculateMA(points, 5)
        val ma20 = TechnicalIndicators.calculateMA(points, 20)
        val ma60 = TechnicalIndicators.calculateMA(points, 60)
        val ma120 = TechnicalIndicators.calculateMA(points, 120)
        val volumeRatio = TechnicalIndicators.calculateVolumeRatio(points)
        val (dif, dea, macd) = TechnicalIndicators.calculateMACD(points)
        val rsiMap = TechnicalIndicators.calculateRSI(points)
        val (bollUpper, bollMiddle, bollLower) = TechnicalIndicators.calculateBOLL(points)
        val snapshots = (maxOf(0, points.size - limit) until points.size).map { i ->
            if (!includeIndicators) return@map points[i].toSnapshot()
            points[i].toSnapshot(
                ma5 = ma5[i],
                ma20 = ma20[i],
                ma60 = ma60[i],
                ma120 = ma120[i],
                volumeRatio = volumeRatio[i],
                dif = dif[i],
                dea = dea[i],
                macd = macd[i],
                rsi6 = rsiMap[6]?.get(i),
                rsi12 = rsiMap[12]?.get(i),
                rsi24 = rsiMap[24]?.get(i),
                bollUpper = bollUpper[i],
                bollMiddle = bollMiddle[i],
                bollLower = bollLower[i],
            )
        }
        return KLineToolSnapshot(
            code = code,
            name = name,
            period = period,
            pointsCount = snapshots.size,
            currency = if (MarketType.fromCode(code) == MarketType.GOLD) "provider_native" else toolCurrency(code),
            source = if (isCached) "CACHE" else "NETWORK",
            asOf = returnedPoints.lastOrNull()?.date,
            requestedLimit = limit,
            latestPoint = snapshots.lastOrNull().takeIf { includeLatest },
            highestHigh = highs.maxOrNull()?.roundedForTool(),
            lowestLow = lows.minOrNull()?.roundedForTool(),
            points = snapshots,
        )
    }

    private fun com.jiucaihua.app.domain.model.KLinePoint.toSnapshot(
        ma5: Double? = null,
        ma20: Double? = null,
        ma60: Double? = null,
        ma120: Double? = null,
        volumeRatio: Double? = null,
        dif: Double? = null,
        dea: Double? = null,
        macd: Double? = null,
        rsi6: Double? = null,
        rsi12: Double? = null,
        rsi24: Double? = null,
        bollUpper: Double? = null,
        bollMiddle: Double? = null,
        bollLower: Double? = null,
    ): KLinePointSnapshot {
        return KLinePointSnapshot(
            date = date,
            open = open.roundedForTool(),
            close = close.roundedForTool(),
            high = high.roundedForTool(),
            low = low.roundedForTool(),
            volume = volume.roundedForTool(),
            changePercent = changePercent.roundedForTool(),
            ma5 = ma5?.roundedForTool(),
            ma20 = ma20?.roundedForTool(),
            ma60 = ma60?.roundedForTool(),
            ma120 = ma120?.roundedForTool(),
            volumeRatio = volumeRatio?.roundedForTool(),
            dif = dif?.roundedForTool(),
            dea = dea?.roundedForTool(),
            macd = macd?.roundedForTool(),
            rsi6 = rsi6?.roundedForTool(),
            rsi12 = rsi12?.roundedForTool(),
            rsi24 = rsi24?.roundedForTool(),
            bollUpper = bollUpper?.roundedForTool(),
            bollMiddle = bollMiddle?.roundedForTool(),
            bollLower = bollLower?.roundedForTool(),
        )
    }

    // Round only the exported snapshot, after indicators use full-precision data.
    private fun Double.roundedForTool(): Double =
        BigDecimal.valueOf(this).setScale(3, RoundingMode.HALF_UP).toDouble()
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
                time = it.epochMillis.takeIf { millis -> millis > 0 }?.let { millis -> java.time.Instant.ofEpochMilli(millis).toString() },
                sourceType = it.sourceType.displayName,
            )
        }
        return MarketNewsDigest(
            generatedAt = java.time.Instant.now().toString(),
            total = items.size,
            limit = limit,
            possiblyTruncated = items.size >= limit,
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
            id = id,
            code = code,
            name = name,
            alertType = alertType.name,
            threshold = threshold,
            actionHint = actionHint,
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
            ?: throw com.jiucaihua.app.ai.tool.ToolExecutionException(com.jiucaihua.app.ai.tool.ToolError("NOT_FOUND", "Holding not found: $code"))
        val currentPrice = holding.currentPrice
        if (currentPrice <= 0) throw com.jiucaihua.app.ai.tool.ToolExecutionException(com.jiucaihua.app.ai.tool.ToolError("PROVIDER_UNAVAILABLE", "Current price unavailable for $code"))

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
