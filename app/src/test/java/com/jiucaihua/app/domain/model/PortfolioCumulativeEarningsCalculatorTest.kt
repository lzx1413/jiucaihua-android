package com.jiucaihua.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioCumulativeEarningsCalculatorTest {

    @Test
    fun `excludes cash transferred in after the reference day`() {
        val result = PortfolioCumulativeEarningsCalculator.calculate(
            currentWealth = 1_700.0,
            referenceSnapshot = snapshot(assetValue = 1_000.0, netExternalCashFlow = 1_000.0),
            currentNetExternalCashFlow = 1_500.0,
            lossCompensation = 50.0,
        )

        assertEquals(150.0, result.earnings, 0.0001)
        assertEquals(15.0, result.earningsPercent, 0.0001)
    }

    @Test
    fun `adds withdrawals back while deducting loss compensation`() {
        val result = PortfolioCumulativeEarningsCalculator.calculate(
            currentWealth = 900.0,
            referenceSnapshot = snapshot(assetValue = 1_000.0, netExternalCashFlow = 1_000.0),
            currentNetExternalCashFlow = 700.0,
            lossCompensation = 20.0,
        )

        assertEquals(180.0, result.earnings, 0.0001)
        assertEquals(18.0, result.earningsPercent, 0.0001)
    }

    private fun snapshot(assetValue: Double, netExternalCashFlow: Double) = PortfolioSnapshot(
        date = "2026-07-19",
        timestamp = 0L,
        totalMarketValue = assetValue,
        totalCost = assetValue,
        totalEarnings = 0.0,
        totalEarningsPercent = 0.0,
        todayEarnings = 0.0,
        cash = 0.0,
        lossCompensation = 0.0,
        categoryValues = emptyMap(),
        netExternalCashFlow = netExternalCashFlow,
    )
}
