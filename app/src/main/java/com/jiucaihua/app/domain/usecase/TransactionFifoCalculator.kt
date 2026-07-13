package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.TransactionHistoryItem
import com.jiucaihua.app.domain.model.TransactionType

object TransactionFifoCalculator {
    fun calculate(transactions: List<InvestmentTransaction>): List<TransactionHistoryItem> {
        val lotsBySecurity = mutableMapOf<String, ArrayDeque<Lot>>()

        return transactions
            .sortedWith(compareBy<InvestmentTransaction> { it.tradeDate }.thenBy { it.id })
            .map { transaction ->
                val lots = lotsBySecurity.getOrPut(transaction.securityKey()) { ArrayDeque() }
                when (transaction.type) {
                    TransactionType.BUY -> {
                        val quantity = transaction.quantity.coerceAtLeast(0.0)
                        val costBasis = transaction.grossAmountCny() + transaction.feeCny + transaction.taxCny
                        if (quantity > EPSILON) {
                            lots.addLast(Lot(quantity, costBasis / quantity))
                        }
                        TransactionHistoryItem(
                            transaction = transaction,
                            costBasisCny = costBasis,
                        )
                    }
                    TransactionType.SELL -> calculateSell(transaction, lots)
                    TransactionType.DIVIDEND -> TransactionHistoryItem(
                        transaction = transaction,
                        dividendIncomeCny = transaction.amountCny - transaction.taxCny,
                    )
                    TransactionType.SPLIT -> {
                        val ratio = transaction.quantity
                        if (ratio > EPSILON) {
                            lots.forEach { lot ->
                                lot.remainingQuantity *= ratio
                                lot.unitCostCny /= ratio
                            }
                        }
                        TransactionHistoryItem(transaction = transaction)
                    }
                    TransactionType.FEE,
                    TransactionType.TAX,
                    TransactionType.CASH_IN,
                    TransactionType.CASH_OUT -> TransactionHistoryItem(transaction = transaction)
                }
            }
    }

    private fun calculateSell(
        transaction: InvestmentTransaction,
        lots: ArrayDeque<Lot>,
    ): TransactionHistoryItem {
        val sellQuantity = transaction.quantity.coerceAtLeast(0.0)
        val proceeds = transaction.grossAmountCny() - transaction.feeCny - transaction.taxCny
        val unitProceeds = if (sellQuantity > EPSILON) proceeds / sellQuantity else 0.0
        var remainingQuantity = sellQuantity
        var matchedQuantity = 0.0
        var costBasis = 0.0

        while (remainingQuantity > EPSILON && lots.isNotEmpty()) {
            val lot = lots.first()
            val consumedQuantity = minOf(remainingQuantity, lot.remainingQuantity)
            costBasis += consumedQuantity * lot.unitCostCny
            matchedQuantity += consumedQuantity
            remainingQuantity -= consumedQuantity
            lot.remainingQuantity -= consumedQuantity
            if (lot.remainingQuantity <= EPSILON) {
                lots.removeFirst()
            }
        }

        val matchedProceeds = matchedQuantity * unitProceeds
        return TransactionHistoryItem(
            transaction = transaction,
            proceedsCny = proceeds,
            costBasisCny = costBasis,
            realizedPnlCny = matchedProceeds - costBasis,
            unmatchedQuantity = remainingQuantity.coerceAtLeast(0.0),
        )
    }

    private fun InvestmentTransaction.grossAmountCny(): Double {
        val nativeAmount = if (amount > 0.0) amount else quantity * price
        return nativeAmount * exchangeRate
    }

    private fun InvestmentTransaction.securityKey(): String {
        return "${marketType?.name.orEmpty()}:${code.orEmpty()}"
    }

    private data class Lot(
        var remainingQuantity: Double,
        var unitCostCny: Double,
    )

    private const val EPSILON = 0.0000001
}
