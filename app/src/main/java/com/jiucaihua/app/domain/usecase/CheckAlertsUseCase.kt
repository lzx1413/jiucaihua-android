package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.StockRepository
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
            }

            if (shouldTrigger) {
                val cooldownMs = 30 * 60 * 1000L
                val lastTriggered = alert.lastTriggeredAt ?: 0
                if (System.currentTimeMillis() - lastTriggered > cooldownMs) {
                    val currentValue = when (alert.alertType) {
                        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> price
                        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> changePercent
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
                        )
                    )
                }
            }
        }

        return triggered
    }

    private val PriceAlert.marketType: MarketType
        get() = MarketType.fromCode(code)
}
