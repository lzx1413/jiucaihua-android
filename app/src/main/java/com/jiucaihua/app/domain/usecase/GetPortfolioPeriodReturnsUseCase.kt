package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.PortfolioPeriodReturn
import com.jiucaihua.app.domain.model.PortfolioPeriodReturnCalculator
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetPortfolioPeriodReturnsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        snapshots: List<PortfolioSnapshot>,
        currentAssetValue: Double,
    ): List<PortfolioPeriodReturn?> {
        return PortfolioPeriodReturnCalculator.calculate(
            snapshots = snapshots,
            currentAssetValue = currentAssetValue,
            transactions = transactionRepository.getAllOnce(),
        )
    }
}
