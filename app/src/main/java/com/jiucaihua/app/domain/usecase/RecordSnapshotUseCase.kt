package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.DailySnapshotSchedule
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.MarketRepository
import com.jiucaihua.app.domain.repository.HoldingSnapshotRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RecordSnapshotUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
    private val holdingSnapshotRepository: HoldingSnapshotRepository,
    private val snapshotRepository: PortfolioSnapshotRepository,
    private val marketRepository: MarketRepository,
    private val marketCalendarRepository: MarketCalendarRepository,
    @Named("appPrefs") private val prefs: SharedPreferences,
) {

    /**
     * Stores one end-of-day portfolio value after A-share and Hong Kong markets close.
     * Intraday refreshes deliberately do not update the return-series baseline.
     */
    suspend fun recordSnapshot(): Long? {
        val now = ZonedDateTime.now(SHANGHAI_ZONE)
        val sessions = marketCalendarRepository.getMarketSessions()
        if (!DailySnapshotSchedule.shouldRecord(now, sessions)) return null

        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        if (summary.holdings.isEmpty()) return null

        val timestamp = now.toInstant().toEpochMilli()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(timestamp)

        // Compute benchmark percent (CSI 300 cumulative return from base)
        val benchmarkPercent = computeBenchmarkPercent()
        val transactionSummary = getTransactionSummaryUseCase(
            TransactionQuery(to = timestamp, limit = Int.MAX_VALUE),
        )
        val netExternalCashFlow = transactionSummary.cashInCny - transactionSummary.cashOutCny

        holdingSnapshotRepository.saveSnapshots(today, timestamp, summary.holdings)

        // Dedup: only keep the latest snapshot for the same day
        val existing = snapshotRepository.getLatest()
        if (existing != null && existing.date == today) {
            // Update: replace with newer snapshot
            return snapshotRepository.saveSnapshot(
                PortfolioSnapshot(
                    id = existing.id,
                    date = today,
                    timestamp = timestamp,
                    totalMarketValue = summary.totalMarketValue,
                    totalCost = summary.totalCost,
                    totalEarnings = summary.totalEarnings,
                    totalEarningsPercent = summary.totalEarningsPercent,
                    todayEarnings = summary.todayEarnings,
                    cash = summary.cash,
                    lossCompensation = summary.lossCompensation,
                    categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
                    benchmarkPercent = benchmarkPercent,
                    netExternalCashFlow = netExternalCashFlow,
                ),
            )
        }

        return snapshotRepository.saveSnapshot(
            PortfolioSnapshot(
                date = today,
                timestamp = timestamp,
                totalMarketValue = summary.totalMarketValue,
                totalCost = summary.totalCost,
                totalEarnings = summary.totalEarnings,
                totalEarningsPercent = summary.totalEarningsPercent,
                todayEarnings = summary.todayEarnings,
                cash = summary.cash,
                lossCompensation = summary.lossCompensation,
                categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
                benchmarkPercent = benchmarkPercent,
                netExternalCashFlow = netExternalCashFlow,
            ),
        )
    }

    /**
     * Compute CSI 300 (沪深300) cumulative return percent from the baseline.
     * Baseline is stored in SharedPreferences as the CSI 300 price when the first snapshot was recorded.
     */
    private suspend fun computeBenchmarkPercent(): Double {
        try {
            val indices = marketRepository.getAStockIndices()
            val csi300 = indices.find { it.code == "sh000300" }
            if (csi300 == null) return 0.0

            val currentPrice = csi300.price
            val basePrice = prefs.getFloat(KEY_BENCHMARK_BASE, 0f)

            if (basePrice <= 0f) {
                // No baseline yet, store current price as base and return 0%
                prefs.edit().putFloat(KEY_BENCHMARK_BASE, currentPrice.toFloat()).apply()
                return 0.0
            }

            return (currentPrice - basePrice.toDouble()) / basePrice.toDouble() * 100
        } catch (_: Exception) {
            return 0.0
        }
    }

    fun getRangeTimestamps(range: ChartRange): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val from = when (range) {
            ChartRange.SEVEN_DAYS -> now - 7 * 24 * 60 * 60 * 1000L
            ChartRange.THIRTY_DAYS -> now - 30 * 24 * 60 * 60 * 1000L
            ChartRange.NINETY_DAYS -> now - 90 * 24 * 60 * 60 * 1000L
            ChartRange.ALL -> 0L
        }
        return Pair(from, now)
    }

    companion object {
        private val SHANGHAI_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
        private const val KEY_CASH = "cash"
        private const val KEY_LOSS_COMPENSATION = "loss_compensation"
        private const val KEY_BENCHMARK_BASE = "benchmark_base_csi300"
    }
}
