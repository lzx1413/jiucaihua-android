package com.jiucaihua.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioReturnCalculatorTest {

    @Test
    fun `excludes external cash deposits from return`() {
        val returns = PortfolioReturnCalculator.returnPercents(
            listOf(
                snapshot(marketValue = 1000.0, cash = 0.0, netExternalCashFlow = 1000.0),
                snapshot(marketValue = 1100.0, cash = 500.0, netExternalCashFlow = 1500.0),
            ),
        )

        assertEquals(0.0, returns[0], 0.0001)
        assertEquals(10.0, returns[1], 0.0001)
    }

    @Test
    fun `adds withdrawals back when calculating return`() {
        val returns = PortfolioReturnCalculator.returnPercents(
            listOf(
                snapshot(marketValue = 1000.0, cash = 0.0, netExternalCashFlow = 1000.0),
                snapshot(marketValue = 800.0, cash = 0.0, netExternalCashFlow = 500.0),
            ),
        )

        assertEquals(30.0, returns[1], 0.0001)
    }

    private fun snapshot(
        marketValue: Double,
        cash: Double,
        netExternalCashFlow: Double,
    ) = PortfolioSnapshot(
        date = "2026-07-19",
        timestamp = 0L,
        totalMarketValue = marketValue,
        totalCost = marketValue,
        totalEarnings = 0.0,
        totalEarningsPercent = 0.0,
        todayEarnings = 0.0,
        cash = cash,
        lossCompensation = 0.0,
        categoryValues = emptyMap(),
        netExternalCashFlow = netExternalCashFlow,
    )
}
