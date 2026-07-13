package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionHistoryItem
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetHoldingTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        code: String,
        marketType: MarketType,
    ): List<TransactionHistoryItem> {
        return TransactionFifoCalculator
            .calculate(transactionRepository.getBySecurity(code, marketType))
            .asReversed()
    }
}
