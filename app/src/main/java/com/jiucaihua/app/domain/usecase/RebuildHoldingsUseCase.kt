package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.TransactionLotMatchRepository
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlin.math.abs

class RebuildHoldingsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionLotMatchRepository: TransactionLotMatchRepository,
    private val holdingRepository: HoldingRepository,
) {
    suspend fun rebuildOne(code: String, marketType: MarketType) {
        val transactions = transactionRepository.getBySecurity(code, marketType)
        val lotMatches = TransactionFifoCalculator.calculate(transactions)
            .flatMap { it.lotMatches }
        transactionLotMatchRepository.replaceForSecurity(code, marketType, lotMatches)
        val projection = project(transactions) ?: return
        val current = holdingRepository.getHoldingByCode(code)
        if (current != null) {
            holdingRepository.updateHolding(projection.copy(id = current.id))
        } else if (!projection.isSoldOut) {
            holdingRepository.addHolding(projection)
        }
    }

    suspend fun rebuildAll() {
        val grouped = transactionRepository.getAllOnce()
            .filter { it.code != null && it.marketType != null }
            .groupBy { it.code.orEmpty() to it.marketType!! }

        grouped.forEach { (key, _) ->
            rebuildOne(key.first, key.second)
        }
    }

    private fun project(transactions: List<InvestmentTransaction>): Holding? {
        if (transactions.isEmpty()) return null

        var shares = 0.0
        var costAmount = 0.0
        var lastCode: String? = null
        var lastName: String? = null
        var lastMarketType: MarketType? = null
        var lastCurrency = "CNY"

        transactions.sortedWith(compareBy<InvestmentTransaction> { it.tradeDate }.thenBy { it.id })
            .forEach { transaction ->
                if (transaction.code != null) lastCode = transaction.code
                if (transaction.name != null) lastName = transaction.name
                if (transaction.marketType != null) lastMarketType = transaction.marketType
                lastCurrency = transaction.currency

                when (transaction.type) {
                    TransactionType.BUY -> {
                        val quantity = transaction.quantity.coerceAtLeast(0.0)
                        val grossAmount = transaction.effectiveAmount()
                        shares += quantity
                        costAmount += grossAmount + transaction.fee + transaction.tax
                    }
                    TransactionType.SELL -> {
                        val quantity = transaction.quantity.coerceAtLeast(0.0).coerceAtMost(shares)
                        if (quantity > 0.0 && shares > 0.0) {
                            val averageCost = costAmount / shares
                            val removedCost = averageCost * quantity
                            shares -= quantity
                            costAmount = (costAmount - removedCost).coerceAtLeast(0.0)
                        }
                    }
                    TransactionType.SPLIT -> {
                        val ratio = transaction.quantity
                        if (ratio > 0.0) {
                            shares *= ratio
                        }
                    }
                    TransactionType.DIVIDEND,
                    TransactionType.FEE,
                    TransactionType.TAX,
                    TransactionType.CASH_IN,
                    TransactionType.CASH_OUT -> Unit
                }
            }

        val code = lastCode ?: return null
        val marketType = lastMarketType ?: return null
        val isSoldOut = shares <= QUANTITY_EPSILON
        val normalizedShares = if (isSoldOut) 0.0 else shares
        val costPrice = if (normalizedShares > 0.0) costAmount / normalizedShares else 0.0

        return Holding(
            code = code,
            name = lastName ?: code,
            marketType = marketType,
            currency = lastCurrency,
            costPrice = if (abs(costPrice) < QUANTITY_EPSILON) 0.0 else costPrice,
            holdingAmount = costPrice * normalizedShares,
            holdingShares = normalizedShares,
            isSoldOut = isSoldOut,
        )
    }

    private fun InvestmentTransaction.effectiveAmount(): Double {
        return if (amount > 0.0) amount else quantity * price
    }

    private companion object {
        const val QUANTITY_EPSILON = 0.00000001
    }
}
