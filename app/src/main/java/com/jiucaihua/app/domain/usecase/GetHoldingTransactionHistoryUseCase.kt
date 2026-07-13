package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionHistoryItem
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.TransactionLotMatchRepository
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetHoldingTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionLotMatchRepository: TransactionLotMatchRepository,
) {
    suspend operator fun invoke(
        code: String,
        marketType: MarketType,
    ): List<TransactionHistoryItem> {
        val historyItems = TransactionFifoCalculator
            .calculate(transactionRepository.getBySecurity(code, marketType))
        val sellIds = historyItems
            .filter { it.transaction.type == TransactionType.SELL && it.transaction.id > 0L }
            .map { it.transaction.id }
        val persistedMatches = transactionLotMatchRepository.getBySellTransactionIds(sellIds)
            .groupBy { it.sellTransactionId }
        val auditedItems = historyItems.map { item ->
            val matches = persistedMatches[item.transaction.id]
            if (matches.isNullOrEmpty()) item else item.copy(lotMatches = matches)
        }
        return auditedItems
            .asReversed()
    }
}
