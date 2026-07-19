package com.jiucaihua.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class PortfolioReturnHistoryCalculatorTest {

    @Test
    fun `groups daily monthly and yearly returns while excluding cash deposits`() {
        val snapshots = listOf(
            snapshot("2025-12-31T16:10:00+08:00", 1000.0),
            snapshot("2026-01-02T16:10:00+08:00", 1100.0),
            snapshot("2026-01-03T16:10:00+08:00", 1600.0),
        )
        val transactions = listOf(
            InvestmentTransaction(
                type = TransactionType.CASH_IN,
                amount = 500.0,
                tradeDate = ZonedDateTime.parse("2026-01-03T10:00:00+08:00").toInstant().toEpochMilli(),
            ),
        )

        val daily = PortfolioReturnHistoryCalculator.calculate(
            snapshots, transactions, ReturnHistoryType.DAILY, "2026-01",
        )
        val monthly = PortfolioReturnHistoryCalculator.calculate(
            snapshots, transactions, ReturnHistoryType.MONTHLY, "2026",
        )
        val yearly = PortfolioReturnHistoryCalculator.calculate(
            snapshots, transactions, ReturnHistoryType.YEARLY,
        )

        assertEquals(listOf("01-03", "01-02"), daily.map { it.label })
        assertEquals(0.0, daily[0].earnings, 0.0001)
        assertEquals(100.0, daily[1].earnings, 0.0001)
        assertEquals(100.0, monthly.single().earnings, 0.0001)
        assertEquals(100.0, yearly.single().earnings, 0.0001)
    }

    private fun snapshot(time: String, assetValue: Double): PortfolioSnapshot = PortfolioSnapshot(
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
    )
}
