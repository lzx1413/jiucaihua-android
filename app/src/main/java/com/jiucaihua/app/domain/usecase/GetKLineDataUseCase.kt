package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.repository.StockRepository
import javax.inject.Inject

class GetKLineDataUseCase @Inject constructor(
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(
        code: String,
        period: KLinePeriod = KLinePeriod.DAILY,
        limit: Int = 120,
    ): KLineData {
        return stockRepository.getKLineData(code, period, limit)
    }
}
