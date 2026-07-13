package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFifoCalculatorTest {

    @Test
    fun calculate_sellAcrossTwoBuyLots_createsAuditableLotMatches() {
        val transactions = listOf(
            transaction(
                id = 1,
                type = TransactionType.BUY,
                tradeDate = 1_000L,
                quantity = 10.0,
                price = 100.0,
                amount = 1_000.0,
            ),
            transaction(
                id = 2,
                type = TransactionType.BUY,
                tradeDate = 2_000L,
                quantity = 10.0,
                price = 110.0,
                amount = 1_100.0,
            ),
            transaction(
                id = 3,
                type = TransactionType.SELL,
                tradeDate = 3_000L,
                quantity = 15.0,
                price = 120.0,
                amount = 1_800.0,
            ),
        )

        val sellItem = TransactionFifoCalculator.calculate(transactions)
            .first { it.transaction.type == TransactionType.SELL }

        assertEquals(1_800.0, sellItem.proceedsCny, 0.0001)
        assertEquals(1_550.0, sellItem.costBasisCny, 0.0001)
        assertEquals(250.0, sellItem.realizedPnlCny, 0.0001)
        assertEquals(2, sellItem.lotMatches.size)

        val firstMatch = sellItem.lotMatches[0]
        assertEquals(1L, firstMatch.buyTransactionId)
        assertEquals(10.0, firstMatch.quantity, 0.0001)
        assertEquals(1_000.0, firstMatch.costBasisCny, 0.0001)
        assertEquals(1_200.0, firstMatch.proceedsCny, 0.0001)
        assertEquals(200.0, firstMatch.realizedPnlCny, 0.0001)

        val secondMatch = sellItem.lotMatches[1]
        assertEquals(2L, secondMatch.buyTransactionId)
        assertEquals(5.0, secondMatch.quantity, 0.0001)
        assertEquals(550.0, secondMatch.costBasisCny, 0.0001)
        assertEquals(600.0, secondMatch.proceedsCny, 0.0001)
        assertEquals(50.0, secondMatch.realizedPnlCny, 0.0001)
    }

    private fun transaction(
        id: Long,
        type: TransactionType,
        tradeDate: Long,
        quantity: Double,
        price: Double,
        amount: Double,
    ): InvestmentTransaction {
        return InvestmentTransaction(
            id = id,
            code = "sh600519",
            name = "贵州茅台",
            marketType = MarketType.A_STOCK,
            type = type,
            tradeDate = tradeDate,
            quantity = quantity,
            price = price,
            amount = amount,
            exchangeRate = 1.0,
        )
    }
}
