package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.PortfolioReturnHistoryCalculator
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.model.ReturnHistoryResult
import com.jiucaihua.app.domain.model.ReturnHistoryType
import com.jiucaihua.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetPortfolioReturnHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        snapshots: List<PortfolioSnapshot>,
        type: ReturnHistoryType,
        selectedOption: String? = null,
        todayEarnings: Double? = null,
    ): ReturnHistoryResult {
        val options = PortfolioReturnHistoryCalculator.availableOptions(snapshots, type)
        val selected = when (type) {
            ReturnHistoryType.YEARLY -> null
            else -> selectedOption?.takeIf { it in options } ?: options.firstOrNull()
        }
        return ReturnHistoryResult(
            type = type,
            selectorOptions = options,
            selectedOption = selected,
            items = PortfolioReturnHistoryCalculator.calculate(
                snapshots = snapshots,
                transactions = transactionRepository.getAllOnce(),
                type = type,
                selectedOption = selected,
                todayEarnings = todayEarnings,
            ),
        )
    }
}
