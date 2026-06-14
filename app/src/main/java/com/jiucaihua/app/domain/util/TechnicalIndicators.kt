package com.jiucaihua.app.domain.util

import com.jiucaihua.app.domain.model.KLinePoint
import kotlin.math.sqrt

object TechnicalIndicators {

    fun calculateMA(points: List<KLinePoint>, period: Int): List<Double?> {
        return points.indices.map { i ->
            if (i < period - 1) null
            else points.subList(i - period + 1, i + 1).map { it.close }.average()
        }
    }

    /**
     * Enrich KLinePoint list with MA5, MA10, MA20 values filled in.
     */
    fun enrichWithMA(points: List<KLinePoint>): List<KLinePoint> {
        val ma5 = calculateMA(points, 5)
        val ma10 = calculateMA(points, 10)
        val ma20 = calculateMA(points, 20)
        return points.mapIndexed { i, point ->
            point.copy(
                ma5 = ma5[i],
                ma10 = ma10[i],
                ma20 = ma20[i],
            )
        }
    }

    fun calculateVolumeRatio(points: List<KLinePoint>): List<Double?> {
        return points.indices.map { i ->
            if (i < 5) null
            else {
                val avgVolume = points.subList(i - 5, i).map { it.volume }.average()
                if (avgVolume <= 0) null else points[i].volume / avgVolume
            }
        }
    }

    fun calculateMACD(
        points: List<KLinePoint>,
        shortPeriod: Int = 12,
        longPeriod: Int = 26,
        signalPeriod: Int = 9,
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val closes = points.map { it.close }
        val emaShort = calculateEMA(closes, shortPeriod)
        val emaLong = calculateEMA(closes, longPeriod)
        val dif = closes.indices.map { i ->
            val s = emaShort[i] ?: return@map null
            val l = emaLong[i] ?: return@map null
            s - l
        }
        val difValues = dif.map { it ?: 0.0 }
        val deaEma = calculateEMA(difValues, signalPeriod)
        val dea = dif.indices.map { i -> deaEma[i] }
        val macd = dif.indices.map { i ->
            val d = dif[i] ?: return@map null
            val e = dea[i] ?: return@map null
            (d - e) * 2
        }
        return Triple(dif, dea, macd)
    }

    fun calculateRSI(points: List<KLinePoint>, periods: List<Int> = listOf(6, 12, 24)): Map<Int, List<Double?>> {
        val closes = points.map { it.close }
        return periods.associateWith { period ->
            if (closes.size < period + 1) {
                closes.indices.map { null }
            } else {
                val rsiList = mutableListOf<Double?>()
                var avgGain = 0.0
                var avgLoss = 0.0
                for (i in closes.indices) {
                    if (i < period) {
                        rsiList.add(null)
                        continue
                    }
                    if (i == period) {
                        var gainSum = 0.0
                        var lossSum = 0.0
                        for (j in 1..period) {
                            val change = closes[j] - closes[j - 1]
                            if (change > 0) gainSum += change else lossSum += -change
                        }
                        avgGain = gainSum / period
                        avgLoss = lossSum / period
                    } else {
                        val change = closes[i] - closes[i - 1]
                        val gain = if (change > 0) change else 0.0
                        val loss = if (change < 0) -change else 0.0
                        avgGain = (avgGain * (period - 1) + gain) / period
                        avgLoss = (avgLoss * (period - 1) + loss) / period
                    }
                    rsiList.add(if (avgLoss == 0.0) 100.0 else 100.0 - 100.0 / (1 + avgGain / avgLoss))
                }
                rsiList
            }
        }
    }

    fun calculateBOLL(points: List<KLinePoint>, period: Int = 20, multiplier: Double = 2.0): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val closes = points.map { it.close }
        val ma = calculateMA(points, period)
        val upper = mutableListOf<Double?>()
        val lower = mutableListOf<Double?>()
        for (i in closes.indices) {
            if (i < period - 1) {
                upper.add(null)
                lower.add(null)
            } else {
                val slice = closes.subList(i - period + 1, i + 1)
                val mean = slice.average()
                val variance = slice.map { (it - mean) * (it - mean) }.sum() / period
                val std = sqrt(variance)
                upper.add(mean + multiplier * std)
                lower.add(mean - multiplier * std)
            }
        }
        return Triple(upper, ma, lower)
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double?> {
        val k = 2.0 / (period + 1)
        val result = mutableListOf<Double?>()
        var ema: Double? = null
        for (i in values.indices) {
            if (i < period - 1) {
                result.add(null)
                continue
            }
            if (ema == null) {
                ema = values.subList(0, period).average()
            } else {
                ema = values[i] * k + ema * (1 - k)
            }
            result.add(ema)
        }
        return result
    }
}
