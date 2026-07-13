package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Named

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val rebuildHoldingsUseCase: RebuildHoldingsUseCase,
    @param:Named("appPrefs") private val prefs: SharedPreferences,
) {
    suspend operator fun invoke(transaction: InvestmentTransaction): Long {
        validate(transaction)
        val savedId = transactionRepository.addTransaction(transaction)
        adjustCash(transaction)
        if (transaction.code != null && transaction.marketType != null) {
            rebuildHoldingsUseCase.rebuildOne(transaction.code, transaction.marketType)
        }
        return savedId
    }

    private fun validate(transaction: InvestmentTransaction) {
        when (transaction.type) {
            TransactionType.BUY,
            TransactionType.SELL -> {
                require(!transaction.code.isNullOrBlank()) { "交易缺少证券代码" }
                require(transaction.marketType != null) { "交易缺少市场类型" }
                require(transaction.quantity > 0.0) { "交易数量必须大于 0" }
                require(transaction.price > 0.0 || transaction.amount > 0.0) { "交易价格或金额必须大于 0" }
            }
            TransactionType.SPLIT -> {
                require(!transaction.code.isNullOrBlank()) { "拆股交易缺少证券代码" }
                require(transaction.marketType != null) { "拆股交易缺少市场类型" }
                require(transaction.quantity > 0.0) { "拆股比例必须大于 0" }
            }
            TransactionType.DIVIDEND,
            TransactionType.FEE,
            TransactionType.TAX,
            TransactionType.CASH_IN,
            TransactionType.CASH_OUT -> {
                require(transaction.amount > 0.0) { "交易金额必须大于 0" }
            }
        }
    }

    private fun adjustCash(transaction: InvestmentTransaction) {
        val cashDiff = when (transaction.type) {
            TransactionType.BUY -> -(transaction.effectiveAmount() + transaction.fee + transaction.tax) * transaction.exchangeRate
            TransactionType.SELL -> (transaction.effectiveAmount() - transaction.fee - transaction.tax) * transaction.exchangeRate
            TransactionType.DIVIDEND -> (transaction.amount - transaction.tax) * transaction.exchangeRate
            TransactionType.FEE -> -transaction.amount * transaction.exchangeRate
            TransactionType.TAX -> -transaction.amount * transaction.exchangeRate
            TransactionType.CASH_IN -> transaction.amount * transaction.exchangeRate
            TransactionType.CASH_OUT -> -transaction.amount * transaction.exchangeRate
            TransactionType.SPLIT -> 0.0
        }
        if (cashDiff == 0.0) return
        val current = prefs.getFloat(KEY_CASH, 0f).toDouble()
        prefs.edit().putFloat(KEY_CASH, (current + cashDiff).toFloat()).apply()
    }

    private fun InvestmentTransaction.effectiveAmount(): Double {
        return if (amount > 0.0) amount else quantity * price
    }

    private companion object {
        const val KEY_CASH = "cash"
    }
}
