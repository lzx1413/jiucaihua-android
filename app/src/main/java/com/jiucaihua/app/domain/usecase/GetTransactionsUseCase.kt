package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(query: TransactionQuery): Pair<Int, List<InvestmentTransaction>> {
        return transactionRepository.count(query) to transactionRepository.query(query)
    }
}
