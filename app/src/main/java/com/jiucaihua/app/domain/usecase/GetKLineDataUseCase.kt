package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.util.TechnicalIndicators
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
        val data = stockRepository.getKLineData(code, period, limit)
        if (data.points.isEmpty()) return data

        val points = data.points
        val ma5 = TechnicalIndicators.calculateMA(points, 5)
        val ma10 = TechnicalIndicators.calculateMA(points, 10)
        val ma20 = TechnicalIndicators.calculateMA(points, 20)

        val enrichedPoints = points.mapIndexed { i, p ->
            p.copy(
                ma5 = ma5[i],
                ma10 = ma10[i],
                ma20 = ma20[i],
            )
        }

        return data.copy(points = enrichedPoints)
    }
}
