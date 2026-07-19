package com.jiucaihua.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class PortfolioPeriodReturnCalculatorTest {

    @Test
    fun `calculates each period from its preceding close and excludes deposits`() {
        val now = ZonedDateTime.parse("2026-07-20T16:30:00+08:00")
        val result = PortfolioPeriodReturnCalculator.calculate(
            snapshots = listOf(
                snapshot("2025-12-31T16:10:00+08:00", 800.0, 1000.0),
                snapshot("2026-06-30T16:10:00+08:00", 900.0, 1000.0),
                snapshot("2026-07-17T16:10:00+08:00", 1000.0, 1000.0),
            ),
            currentAssetValue = 1650.0,
            transactions = listOf(cashIn("2026-07-20T10:00:00+08:00", 500.0)),
            now = now,
        )

        assertEquals(150.0, result[ReturnPeriod.DAY.ordinal]!!.earnings, 0.0001)
        assertEquals(250.0, result[ReturnPeriod.MONTH.ordinal]!!.earnings, 0.0001)
        assertEquals(350.0, result[ReturnPeriod.YEAR.ordinal]!!.earnings, 0.0001)
    }

    @Test
    fun `returns null when no snapshot exists before period start`() {
        val result = PortfolioPeriodReturnCalculator.calculate(
            snapshots = listOf(snapshot("2026-07-20T16:10:00+08:00", 1000.0, 1000.0)),
            currentAssetValue = 1000.0,
            transactions = emptyList(),
            now = ZonedDateTime.parse("2026-07-20T16:30:00+08:00"),
        )

        assertNull(result[ReturnPeriod.DAY.ordinal])
    }

    private fun snapshot(time: String, assetValue: Double, netExternalCashFlow: Double): PortfolioSnapshot {
        return PortfolioSnapshot(
            date = time.take(10),
            timestamp = ZonedDateTime.parse(time).toInstant().toEpochMilli(),
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

    private fun cashIn(time: String, amount: Double): InvestmentTransaction {
        return InvestmentTransaction(
            type = TransactionType.CASH_IN,
            tradeDate = ZonedDateTime.parse(time).toInstant().toEpochMilli(),
            amount = amount,
        )
    }
}
