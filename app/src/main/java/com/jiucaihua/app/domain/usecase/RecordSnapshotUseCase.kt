package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RecordSnapshotUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val snapshotRepository: PortfolioSnapshotRepository,
    @Named("appPrefs") private val prefs: SharedPreferences,
) {

    suspend fun recordSnapshot(): Long? {
        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        if (summary.holdings.isEmpty()) return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(System.currentTimeMillis())

        // Dedup: only keep the latest snapshot for the same day
        val existing = snapshotRepository.getLatest()
        if (existing != null && existing.date == today) {
            // Update: replace with newer snapshot
            return snapshotRepository.saveSnapshot(
                PortfolioSnapshot(
                    id = existing.id,
                    date = today,
                    timestamp = System.currentTimeMillis(),
                    totalMarketValue = summary.totalMarketValue,
                    totalCost = summary.totalCost,
                    totalEarnings = summary.totalEarnings,
                    totalEarningsPercent = summary.totalEarningsPercent,
                    todayEarnings = summary.todayEarnings,
                    cash = summary.cash,
                    lossCompensation = summary.lossCompensation,
                    categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
                ),
            )
        }

        return snapshotRepository.saveSnapshot(
            PortfolioSnapshot(
                date = today,
                timestamp = System.currentTimeMillis(),
                totalMarketValue = summary.totalMarketValue,
                totalCost = summary.totalCost,
                totalEarnings = summary.totalEarnings,
                totalEarningsPercent = summary.totalEarningsPercent,
                todayEarnings = summary.todayEarnings,
                cash = summary.cash,
                lossCompensation = summary.lossCompensation,
                categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
            ),
        )
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
        private const val KEY_CASH = "cash"
        private const val KEY_LOSS_COMPENSATION = "loss_compensation"
    }
}
