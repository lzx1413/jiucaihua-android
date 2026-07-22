package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.repository.HoldingSnapshotRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Starts performance tracking from the current portfolio value.
 * Transaction and holding history are deliberately retained; only return snapshots
 * and the historical loss adjustment are reset.
 */
class ResetPortfolioReferenceDateUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
    private val holdingSnapshotRepository: HoldingSnapshotRepository,
    private val snapshotRepository: PortfolioSnapshotRepository,
    @Named("appPrefs") private val prefs: SharedPreferences,
) {
    suspend operator fun invoke(): Boolean {
        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        if (summary.holdings.isEmpty()) return false

        val timestamp = System.currentTimeMillis()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        val transactionSummary = getTransactionSummaryUseCase()

        snapshotRepository.clearAll()
        holdingSnapshotRepository.saveSnapshots(date, timestamp, summary.holdings)
        snapshotRepository.saveSnapshot(
            PortfolioSnapshot(
                date = date,
                timestamp = timestamp,
                totalMarketValue = summary.totalMarketValue,
                totalCost = summary.totalCost,
                totalEarnings = summary.totalEarnings,
                totalEarningsPercent = summary.totalEarningsPercent,
                todayEarnings = summary.todayEarnings,
                cash = summary.cash,
                lossCompensation = 0.0,
                categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
                netExternalCashFlow = transactionSummary.cashInCny - transactionSummary.cashOutCny,
            ),
        )
        prefs.edit()
            .remove(KEY_LOSS_COMPENSATION)
            .remove(KEY_BENCHMARK_BASE)
            .putLong(KEY_REFERENCE_RESET_AT, timestamp)
            .apply()
        return true
    }

    private companion object {
        const val KEY_LOSS_COMPENSATION = "loss_compensation"
        const val KEY_BENCHMARK_BASE = "benchmark_base_csi300"
        const val KEY_REFERENCE_RESET_AT = "portfolio_reference_reset_at"
    }
}
