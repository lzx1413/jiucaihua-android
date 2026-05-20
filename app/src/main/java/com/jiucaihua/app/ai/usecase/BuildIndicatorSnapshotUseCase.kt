package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.IndicatorSnapshot
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
import javax.inject.Inject

class BuildIndicatorSnapshotUseCase @Inject constructor(
    private val getKLineDataUseCase: GetKLineDataUseCase,
    private val fundRepository: FundRepository,
) {
    // 同一轮对话内缓存沪深300 MA20，避免重复请求
    @Volatile
    private var hs300Cache: Hs300Cache? = null

    suspend operator fun invoke(
        code: String,
        costPrice: Double?,
        holdDays: Int?,
    ): IndicatorSnapshot {
        val marketType = MarketType.fromCode(code)
        val data = if (marketType == MarketType.FUND) {
            fundRepository.getFundNavHistory(code, DATA_POINTS)
        } else {
            getKLineDataUseCase(code, com.jiucaihua.app.domain.model.KLinePeriod.DAILY, DATA_POINTS)
        }
        return buildSnapshot(data, marketType, costPrice, holdDays)
    }

    private suspend fun buildSnapshot(
        data: KLineData,
        marketType: MarketType,
        costPrice: Double?,
        holdDays: Int?,
    ): IndicatorSnapshot {
        val points = data.points
        if (points.isEmpty()) {
            return IndicatorSnapshot(code = data.code, name = data.name, price = 0.0, date = "")
        }

        val last = points.last()
        val price = last.close

        // 计算均线
        val ma5 = TechnicalIndicators.calculateMA(points, 5)
        val ma20 = TechnicalIndicators.calculateMA(points, 20)
        val ma60 = TechnicalIndicators.calculateMA(points, 60)
        val ma120 = TechnicalIndicators.calculateMA(points, 120)

        val lastMa5 = ma5.lastOrNull { it != null }
        val lastMa20 = ma20.lastOrNull { it != null }
        val lastMa60 = ma60.lastOrNull { it != null }
        val lastMa120 = ma120.lastOrNull { it != null }

        // vs_ma20/60/120
        val vsMa20 = lastMa20?.let { if (price > it) "above" else "below" }
        val vsMa60 = lastMa60?.let { if (price > it) "above" else "below" }
        val vsMa120 = lastMa120?.let { if (price > it) "above" else "below" }

        // MA20斜率
        val ma20Slope = calcMa20Slope(ma20)

        // 距离均线百分比
        val distToMa20 = lastMa20?.let { (price - it) / it * 100 }
        val distToMa60 = lastMa60?.let { (price - it) / it * 100 }

        // 量比
        val volumeRatio = TechnicalIndicators.calculateVolumeRatio(points)
        val lastVolumeRatio = volumeRatio.lastOrNull { it != null }

        // 10日涨跌幅
        val change10d = if (points.size >= 11) {
            val close10dAgo = points[points.size - 11].close
            if (close10dAgo > 0) (price - close10dAgo) / close10dAgo * 100 else null
        } else null

        // 布林线
        val (bollUpper, bollMiddle, bollLower) = TechnicalIndicators.calculateBOLL(points)
        val lastBollMiddle = bollMiddle.lastOrNull { it != null }
        val lastBollUpper = bollUpper.lastOrNull { it != null }
        val lastBollLower = bollLower.lastOrNull { it != null }
        val bollPosition = lastBollMiddle?.let { if (price > it) "above_mid" else "below_mid" }

        // MACD
        val (dif, dea, _) = TechnicalIndicators.calculateMACD(points)
        val macdStatus = calcMacdStatus(dif, dea)

        // 沪深300 vs MA20
        val hs300VsMa20 = if (marketType == MarketType.A_STOCK) {
            getHs300VsMa20()
        } else "unknown"

        // 持仓相关
        var currentPnlPct: Double? = null
        var peakPnlPct: Double? = null
        var pnlDrawdownFromPeak: Double? = null
        var stopLossPrice: Double? = null
        var distToStopLoss: Double? = null
        var distToPnl30: Double? = null
        var distToPnl50: Double? = null

        if (costPrice != null && costPrice > 0) {
            currentPnlPct = (price - costPrice) / costPrice * 100
            stopLossPrice = costPrice * 0.92
            distToStopLoss = (price - stopLossPrice) / stopLossPrice * 100
            distToPnl30 = 30.0 - currentPnlPct
            distToPnl50 = 50.0 - currentPnlPct

            if (holdDays != null && holdDays > 0) {
                val startIndex = maxOf(0, points.size - holdDays)
                val holdHighs = points.subList(startIndex, points.size).map { it.high }
                val holdPeriodHigh = holdHighs.maxOrNull() ?: price
                peakPnlPct = (holdPeriodHigh - costPrice) / costPrice * 100

                if (peakPnlPct > 0) {
                    pnlDrawdownFromPeak = (peakPnlPct - currentPnlPct) / peakPnlPct * 100
                }
            }
        }

        return IndicatorSnapshot(
            code = data.code,
            name = data.name,
            price = price,
            date = last.date,
            ma5 = lastMa5,
            ma20 = lastMa20,
            ma60 = lastMa60,
            ma120 = lastMa120,
            vs_ma20 = vsMa20,
            vs_ma60 = vsMa60,
            vs_ma120 = vsMa120,
            ma20_slope = ma20Slope,
            distance_to_ma20_pct = distToMa20,
            distance_to_ma60_pct = distToMa60,
            volume_ratio = lastVolumeRatio,
            change_10d_pct = change10d,
            boll_position = bollPosition,
            boll_upper = lastBollUpper,
            boll_middle = lastBollMiddle,
            boll_lower = lastBollLower,
            macd_status = macdStatus,
            hs300_vs_ma20 = hs300VsMa20,
            current_pnl_pct = currentPnlPct,
            peak_pnl_pct = peakPnlPct,
            pnl_drawdown_from_peak = pnlDrawdownFromPeak,
            stop_loss_price = stopLossPrice,
            distance_to_stop_loss_pct = distToStopLoss,
            distance_to_pnl_30_pct = distToPnl30,
            distance_to_pnl_50_pct = distToPnl50,
        )
    }

    private fun calcMa20Slope(ma20: List<Double?>): String? {
        val validValues = ma20.mapIndexedNotNull { index, value ->
            value?.let { index to it }
        }
        if (validValues.size < 3) return null
        val (i0, v0) = validValues[validValues.size - 3]
        val (_, v2) = validValues[validValues.size - 1]
        if (v0 == 0.0) return null
        val changeRate = (v2 - v0) / v0
        return when {
            changeRate > 0.001 -> "up"
            changeRate < -0.001 -> "down"
            else -> "flat"
        }
    }

    private fun calcMacdStatus(dif: List<Double?>, dea: List<Double?>): String? {
        val recentN = 5
        if (dif.size < recentN + 1 || dea.size < recentN + 1) return null

        // 检查近5日内是否有交叉
        val startIdx = dif.size - recentN
        for (i in startIdx until dif.size) {
            val dPrev = dif[i - 1] ?: continue
            val ePrev = dea[i - 1] ?: continue
            val dCurr = dif[i] ?: continue
            val eCurr = dea[i] ?: continue
            // DIF从下方穿越DEA = 金叉
            if (dPrev <= ePrev && dCurr > eCurr) return "golden_cross"
            // DIF从上方穿越DEA = 死叉
            if (dPrev >= ePrev && dCurr < eCurr) return "death_cross"
        }

        // 无交叉则看DIF符号
        val lastDif = dif.lastOrNull { it != null } ?: return null
        return if (lastDif > 0) "above_zero" else "below_zero"
    }

    private suspend fun getHs300VsMa20(): String {
        val cached = hs300Cache
        if (cached != null) return cached.vsMa20

        return try {
            val data = getKLineDataUseCase("sh000300", com.jiucaihua.app.domain.model.KLinePeriod.DAILY, 25)
            if (data.points.isEmpty()) {
                val result = "unknown"
                hs300Cache = Hs300Cache(result)
                return result
            }
            val ma20 = TechnicalIndicators.calculateMA(data.points, 20)
            val lastMa20 = ma20.lastOrNull { it != null }
            val lastClose = data.points.last().close
            val result = lastMa20?.let { if (lastClose > it) "above" else "below" } ?: "unknown"
            hs300Cache = Hs300Cache(result)
            result
        } catch (_: Exception) {
            val result = "unknown"
            hs300Cache = Hs300Cache(result)
            result
        }
    }

    private data class Hs300Cache(val vsMa20: String)

    companion object {
        // MA60需60个前置点 + 5个用于slope计算
        private const val DATA_POINTS = 65
    }
}
