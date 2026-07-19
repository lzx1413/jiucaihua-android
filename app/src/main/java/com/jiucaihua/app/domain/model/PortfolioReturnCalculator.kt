package com.jiucaihua.app.domain.model

/**
 * Calculates the portfolio return relative to the first snapshot in a displayed range.
 * External deposits and withdrawals are excluded so that only investment performance is shown.
 */
object PortfolioReturnCalculator {
    fun returnPercents(snapshots: List<PortfolioSnapshot>): List<Double> {
        val base = snapshots.firstOrNull() ?: return emptyList()
        val baseAssets = base.assetValue()
        if (baseAssets <= 0.0) return List(snapshots.size) { 0.0 }

        return snapshots.map { snapshot ->
            val assetChange = snapshot.assetValue() - baseAssets
            val netCashFlowChange = snapshot.netExternalCashFlow - base.netExternalCashFlow
            (assetChange - netCashFlowChange) / baseAssets * 100.0
        }
    }

    private fun PortfolioSnapshot.assetValue(): Double = totalMarketValue + cash
}
