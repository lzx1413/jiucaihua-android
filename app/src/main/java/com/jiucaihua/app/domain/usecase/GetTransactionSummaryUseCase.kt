package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.model.TransactionSummary
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(query: TransactionQuery = TransactionQuery(limit = Int.MAX_VALUE)): TransactionSummary {
        val transactions = transactionRepository.query(query.copy(limit = MAX_ANALYSIS_ROWS, offset = 0))
            .sortedWith(compareBy<InvestmentTransaction> { it.tradeDate }.thenBy { it.id })

        var buyAmount = 0.0
        var sellAmount = 0.0
        var realizedPnl = 0.0
        var dividendIncome = 0.0
        var fees = 0.0
        var taxes = 0.0
        var cashIn = 0.0
        var cashOut = 0.0
        var buyCount = 0
        var sellCount = 0

        val fifoItems = TransactionFifoCalculator.calculate(transactions)
        realizedPnl = fifoItems.sumOf { it.realizedPnlCny }

        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.BUY -> {
                    buyCount += 1
                    val amount = transaction.effectiveAmountCny()
                    buyAmount += amount
                    fees += transaction.feeCny
                    taxes += transaction.taxCny
                }
                TransactionType.SELL -> {
                    sellCount += 1
                    val grossAmount = transaction.effectiveAmountCny()
                    sellAmount += grossAmount
                    fees += transaction.feeCny
                    taxes += transaction.taxCny
                }
                TransactionType.DIVIDEND -> {
                    dividendIncome += transaction.amountCny
                    taxes += transaction.taxCny
                }
                TransactionType.FEE -> fees += transaction.amountCny
                TransactionType.TAX -> taxes += transaction.amountCny
                TransactionType.CASH_IN -> cashIn += transaction.amountCny
                TransactionType.CASH_OUT -> cashOut += transaction.amountCny
                TransactionType.SPLIT -> Unit
            }
        }

        return TransactionSummary(
            buyAmountCny = buyAmount,
            sellAmountCny = sellAmount,
            netInvestmentCny = buyAmount - sellAmount,
            realizedPnlCny = realizedPnl,
            dividendIncomeCny = dividendIncome,
            feesCny = fees,
            taxesCny = taxes,
            cashInCny = cashIn,
            cashOutCny = cashOut,
            tradeCount = transactions.size,
            buyCount = buyCount,
            sellCount = sellCount,
            firstTradeDate = transactions.firstOrNull()?.tradeDate,
            lastTradeDate = transactions.lastOrNull()?.tradeDate,
        )
    }

    private fun InvestmentTransaction.effectiveAmountCny(): Double {
        val nativeAmount = if (amount > 0.0) amount else quantity * price
        return nativeAmount * exchangeRate
    }

    private companion object {
        const val MAX_ANALYSIS_ROWS = 5000
    }
}
