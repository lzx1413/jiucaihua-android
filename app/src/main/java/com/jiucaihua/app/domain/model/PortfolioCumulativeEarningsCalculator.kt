package com.jiucaihua.app.domain.model

/**
 * Calculates cumulative earnings from the first recorded portfolio value.
 *
 * Cash transferred in or out after the reference snapshot is removed from the
 * wealth change, so only investment performance is counted as earnings.
 */
object PortfolioCumulativeEarningsCalculator {
    fun calculate(
        currentWealth: Double,
        referenceSnapshot: PortfolioSnapshot,
        currentNetExternalCashFlow: Double,
        lossCompensation: Double,
    ): PortfolioCumulativeEarnings {
        val referenceWealth = referenceSnapshot.totalMarketValue + referenceSnapshot.cash
        val netExternalCashFlowSinceReference =
            currentNetExternalCashFlow - referenceSnapshot.netExternalCashFlow
        val earnings = currentWealth - referenceWealth - netExternalCashFlowSinceReference - lossCompensation
        return PortfolioCumulativeEarnings(
            earnings = earnings,
            earningsPercent = if (referenceWealth > 0.0) earnings / referenceWealth * 100.0 else 0.0,
        )
    }
}

data class PortfolioCumulativeEarnings(
    val earnings: Double,
    val earningsPercent: Double,
)
