package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.util.TechnicalIndicators
import javax.inject.Inject

data class TriggeredAlert(
    val alert: PriceAlert,
    val currentValue: Double,
)

class CheckAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
    private val marketCalendarRepository: MarketCalendarRepository,
) {

    suspend fun checkAlerts(): List<TriggeredAlert> {
        val alerts = alertRepository.getEnabledAlerts()
        if (alerts.isEmpty()) return emptyList()

        val sessions = marketCalendarRepository.getMarketSessions()
        val tradingMarkets = sessions.filterValues { it == MarketSession.TRADING }.keys

        val triggered = mutableListOf<TriggeredAlert>()

        // Separate alerts by type: price-based and KLine-based
        val priceAlerts = alerts.filter { it.alertType in PRICE_BASED_TYPES }
        val klineAlerts = alerts.filter { it.alertType in KLINE_BASED_TYPES }

        // Check price-based alerts
        triggered.addAll(checkPriceAlerts(priceAlerts, tradingMarkets))

        // Check KLine-based alerts
        triggered.addAll(checkKLineAlerts(klineAlerts, tradingMarkets))

        return triggered
    }

    private suspend fun checkPriceAlerts(
        alerts: List<PriceAlert>,
        tradingMarkets: Set<MarketType>,
    ): List<TriggeredAlert> {
        if (alerts.isEmpty()) return emptyList()

        val triggered = mutableListOf<TriggeredAlert>()

        val aStockCodes = alerts.filter { it.marketType == MarketType.A_STOCK && MarketType.A_STOCK in tradingMarkets }.map { it.code }.distinct()
        val hkStockCodes = alerts.filter { it.marketType == MarketType.HK_STOCK && MarketType.HK_STOCK in tradingMarkets }.map { it.code }.distinct()
        val usStockCodes = alerts.filter { it.marketType == MarketType.US_STOCK && MarketType.US_STOCK in tradingMarkets }.map { it.code }.distinct()
        val goldCodes = alerts.filter { it.marketType == MarketType.GOLD && MarketType.GOLD in tradingMarkets }.map { it.code }.distinct()
        val fundCodes = alerts.filter { it.marketType == MarketType.FUND && MarketType.A_STOCK in tradingMarkets }.map { it.code }.distinct()

        val stockQuotes = buildMap {
            if (aStockCodes.isNotEmpty()) {
                try { stockRepository.getAStockQuotes(aStockCodes) } catch (_: Exception) { stockRepository.getCachedQuotes(aStockCodes) }
                    .forEach { put(it.code, it) }
            }
            if (hkStockCodes.isNotEmpty()) {
                try { stockRepository.getHKStockQuotes(hkStockCodes) } catch (_: Exception) { stockRepository.getCachedQuotes(hkStockCodes) }
                    .forEach { put(it.code, it) }
            }
            if (usStockCodes.isNotEmpty()) {
                try { stockRepository.getUSStockQuotes(usStockCodes) } catch (_: Exception) { stockRepository.getCachedQuotes(usStockCodes) }
                    .forEach { put(it.code, it) }
            }
            if (goldCodes.isNotEmpty()) {
                try { stockRepository.getGoldQuotes(goldCodes) } catch (_: Exception) { stockRepository.getCachedQuotes(goldCodes) }
                    .forEach { put(it.code, it) }
            }
        }

        val fundQuotes = if (fundCodes.isNotEmpty()) {
            try { fundRepository.getFundQuotes(fundCodes) } catch (_: Exception) { fundRepository.getCachedFundQuotes(fundCodes) }
                .associateBy { it.code }
        } else emptyMap()

        for (alert in alerts) {
            val marketType = alert.marketType
            if (marketType != MarketType.FUND && marketType !in tradingMarkets) continue

            val (price, changePercent) = when (marketType) {
                MarketType.FUND -> {
                    val fq = fundQuotes[alert.code] ?: continue
                    fq.estimatedValue to fq.dailyChangePercent
                }
                else -> {
                    val sq = stockQuotes[alert.code] ?: continue
                    sq.price to sq.changePercent
                }
            }

            val shouldTrigger = when (alert.alertType) {
                AlertType.PRICE_ABOVE -> price >= alert.threshold
                AlertType.PRICE_BELOW -> price <= alert.threshold
                AlertType.CHANGE_ABOVE -> changePercent >= alert.threshold
                AlertType.CHANGE_BELOW -> changePercent <= -alert.threshold
                else -> false // KLine-based types handled separately
            }

            if (shouldTrigger) {
                val cooldownMs = 30 * 60 * 1000L
                val lastTriggered = alert.lastTriggeredAt ?: 0
                if (System.currentTimeMillis() - lastTriggered > cooldownMs) {
                    val currentValue = when (alert.alertType) {
                        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> price
                        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> changePercent
                        else -> 0.0
                    }
                    triggered.add(TriggeredAlert(alert, currentValue))
                    alertRepository.markTriggered(alert.id)
                    alertRepository.addAlertRecord(
                        AlertRecord(
                            alertId = alert.id,
                            code = alert.code,
                            name = alert.name,
                            alertType = alert.alertType,
                            threshold = alert.threshold,
                            currentValue = currentValue,
                            actionHint = alert.actionHint,
                            params = alert.params,
                        )
                    )
                }
            }
        }

        return triggered
    }

    private suspend fun checkKLineAlerts(
        alerts: List<PriceAlert>,
        tradingMarkets: Set<MarketType>,
    ): List<TriggeredAlert> {
        if (alerts.isEmpty()) return emptyList()

        val triggered = mutableListOf<TriggeredAlert>()

        // Filter alerts for markets that are trading
        val activeAlerts = alerts.filter { alert ->
            val marketType = alert.marketType
            marketType == MarketType.FUND && MarketType.A_STOCK in tradingMarkets ||
                marketType in tradingMarkets
        }

        // Group by code for batch KLine fetching
        val alertsByCode = activeAlerts.groupBy { it.code }

        // Fetch KLine data for each unique code
        val klineDataByCode = mutableMapOf<String, KLineData>()
        for (code in alertsByCode.keys) {
            try {
                val data = stockRepository.getKLineData(code, KLinePeriod.DAILY, 60)
                if (data.points.isNotEmpty()) {
                    // Enrich with MA values
                    val enrichedPoints = TechnicalIndicators.enrichWithMA(data.points)
                    klineDataByCode[code] = data.copy(points = enrichedPoints)
                }
            } catch (_: Exception) {
                // Skip if KLine data unavailable
            }
        }

        for ((code, codeAlerts) in alertsByCode) {
            val klineData = klineDataByCode[code] ?: continue
            val points = klineData.points
            if (points.isEmpty()) continue

            val lastPoint = points.last()

            for (alert in codeAlerts) {
                val shouldTrigger = checkKLineCondition(alert, points, lastPoint)
                if (shouldTrigger != null) {
                    val cooldownMs = 30 * 60 * 1000L
                    val lastTriggered = alert.lastTriggeredAt ?: 0
                    if (System.currentTimeMillis() - lastTriggered > cooldownMs) {
                        triggered.add(TriggeredAlert(alert, shouldTrigger))
                        alertRepository.markTriggered(alert.id)
                        alertRepository.addAlertRecord(
                            AlertRecord(
                                alertId = alert.id,
                                code = alert.code,
                                name = alert.name,
                                alertType = alert.alertType,
                                threshold = alert.threshold,
                                currentValue = shouldTrigger,
                                actionHint = alert.actionHint,
                                params = alert.params,
                            )
                        )
                    }
                }
            }
        }

        return triggered
    }

    /**
     * Check KLine-based alert conditions.
     * Returns the current metric value if triggered, null otherwise.
     */
    private fun checkKLineCondition(
        alert: PriceAlert,
        points: List<com.jiucaihua.app.domain.model.KLinePoint>,
        lastPoint: com.jiucaihua.app.domain.model.KLinePoint,
    ): Double? {
        when (alert.alertType) {
            AlertType.VOLUME_ABOVE -> {
                if (lastPoint.volume > alert.threshold) {
                    return lastPoint.volume
                }
            }
            AlertType.NEW_HIGH -> {
                val period = alert.params["period"]?.toIntOrNull() ?: 20
                if (points.size >= period) {
                    val recentPoints = points.takeLast(period)
                    val maxClose = recentPoints.maxOf { it.close }
                    if (lastPoint.close >= maxClose) {
                        return lastPoint.close
                    }
                }
            }
            AlertType.NEW_LOW -> {
                val period = alert.params["period"]?.toIntOrNull() ?: 20
                if (points.size >= period) {
                    val recentPoints = points.takeLast(period)
                    val minClose = recentPoints.minOf { it.close }
                    if (lastPoint.close <= minClose) {
                        return lastPoint.close
                    }
                }
            }
            AlertType.MA_CROSS_ABOVE -> {
                val shortPeriod = alert.params["short_period"]?.toIntOrNull() ?: 5
                val longPeriod = alert.params["long_period"]?.toIntOrNull() ?: 20
                if (points.size >= longPeriod + 1) {
                    val shortMA = TechnicalIndicators.calculateMA(points, shortPeriod)
                    val longMA = TechnicalIndicators.calculateMA(points, longPeriod)
                    val lastShortMA = shortMA.lastOrNull()
                    val lastLongMA = longMA.lastOrNull()
                    val prevShortMA = shortMA.getOrNull(shortMA.size - 2)
                    val prevLongMA = longMA.getOrNull(longMA.size - 2)
                    if (lastShortMA != null && lastLongMA != null && prevShortMA != null && prevLongMA != null) {
                        // Golden cross: short MA crosses above long MA
                        if (prevShortMA <= prevLongMA && lastShortMA > lastLongMA) {
                            return lastShortMA
                        }
                    }
                }
            }
            AlertType.MA_CROSS_BELOW -> {
                val shortPeriod = alert.params["short_period"]?.toIntOrNull() ?: 5
                val longPeriod = alert.params["long_period"]?.toIntOrNull() ?: 20
                if (points.size >= longPeriod + 1) {
                    val shortMA = TechnicalIndicators.calculateMA(points, shortPeriod)
                    val longMA = TechnicalIndicators.calculateMA(points, longPeriod)
                    val lastShortMA = shortMA.lastOrNull()
                    val lastLongMA = longMA.lastOrNull()
                    val prevShortMA = shortMA.getOrNull(shortMA.size - 2)
                    val prevLongMA = longMA.getOrNull(longMA.size - 2)
                    if (lastShortMA != null && lastLongMA != null && prevShortMA != null && prevLongMA != null) {
                        // Death cross: short MA crosses below long MA
                        if (prevShortMA >= prevLongMA && lastShortMA < lastLongMA) {
                            return lastShortMA
                        }
                    }
                }
            }
            else -> return null
        }
        return null
    }

    private val PriceAlert.marketType: MarketType
        get() = MarketType.fromCode(code)

    companion object {
        private val PRICE_BASED_TYPES = setOf(
            AlertType.PRICE_ABOVE,
            AlertType.PRICE_BELOW,
            AlertType.CHANGE_ABOVE,
            AlertType.CHANGE_BELOW,
        )

        private val KLINE_BASED_TYPES = setOf(
            AlertType.VOLUME_ABOVE,
            AlertType.NEW_HIGH,
            AlertType.NEW_LOW,
            AlertType.MA_CROSS_ABOVE,
            AlertType.MA_CROSS_BELOW,
        )
    }
}
