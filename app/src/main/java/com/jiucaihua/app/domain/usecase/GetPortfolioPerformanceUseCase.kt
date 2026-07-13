package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.PortfolioPerformance
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import javax.inject.Inject

class GetPortfolioPerformanceUseCase @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
    private val snapshotRepository: PortfolioSnapshotRepository,
) {
    suspend operator fun invoke(from: Long? = null, to: Long? = null): PortfolioPerformance {
        val summary = getPortfolioUseCase.getPortfolioWithQuotes()
        val holdingsMarketValue = summary.totalMarketValue
        val cash = summary.cash
        val endValue = holdingsMarketValue + cash
        val snapshots = snapshotRepository.getAllOnce()
        val startSnapshot = findStartSnapshot(snapshots, from)
        val effectiveFrom = from ?: startSnapshot?.timestamp
        val transactionSummary = getTransactionSummaryUseCase(
            TransactionQuery(from = effectiveFrom, to = to, limit = Int.MAX_VALUE),
        )
        val periodReturn = transactionSummary.realizedPnlCny +
            (summary.totalMarketValue - summary.totalCost) +
            transactionSummary.dividendIncomeCny -
            transactionSummary.feesCny -
            transactionSummary.taxesCny
        val netExternalFlow = transactionSummary.cashInCny - transactionSummary.cashOutCny
        val startValue = startSnapshot?.assetValue()
            ?: (endValue - netExternalFlow - periodReturn).coerceAtLeast(0.0)
        val absoluteReturn = endValue - startValue - netExternalFlow
        val twr = if (startValue > 0.0) absoluteReturn / startValue * 100.0 else 0.0

        return PortfolioPerformance(
            startDate = effectiveFrom ?: transactionSummary.firstTradeDate ?: 0L,
            endDate = to ?: System.currentTimeMillis(),
            startValue = startValue,
            endValue = endValue,
            externalCashIn = transactionSummary.cashInCny,
            externalCashOut = transactionSummary.cashOutCny,
            netExternalFlow = netExternalFlow,
            absoluteReturn = absoluteReturn,
            twr = twr,
            realizedPnl = transactionSummary.realizedPnlCny,
            unrealizedPnl = summary.totalMarketValue - summary.totalCost,
            dividendIncome = transactionSummary.dividendIncomeCny,
            fees = transactionSummary.feesCny,
            taxes = transactionSummary.taxesCny,
            cash = cash,
            holdingsMarketValue = holdingsMarketValue,
        )
    }

    private fun findStartSnapshot(
        snapshots: List<PortfolioSnapshot>,
        from: Long?,
    ): PortfolioSnapshot? {
        if (snapshots.isEmpty()) return null
        val sortedSnapshots = snapshots.sortedBy { it.timestamp }
        return if (from != null) {
            sortedSnapshots.lastOrNull { it.timestamp <= from }
                ?: sortedSnapshots.firstOrNull { it.timestamp >= from }
        } else {
            sortedSnapshots.firstOrNull()
        }
    }

    private fun PortfolioSnapshot.assetValue(): Double = totalMarketValue + cash
}
