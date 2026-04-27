package com.jiucaihua.app.data.parser

import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import org.json.JSONArray
import org.json.JSONObject

object StockDataParser {

    fun toTencentKLineSymbol(code: String): String {
        return when {
            code.startsWith("hk") -> {
                val suffix = code.removePrefix("hk")
                if (suffix.all { it.isDigit() }) "hk${suffix.padStart(5, '0')}" else "hk${suffix.uppercase()}"
            }
            code.startsWith("sh") -> code
            code.startsWith("sz") -> code
            else -> code
        }
    }

    fun parseTencentKLineResponse(code: String, symbol: String, period: KLinePeriod, response: String): KLineData {
        val json = JSONObject(response)
        val data = json.optJSONObject("data") ?: return KLineData(code, "", period, emptyList())
        val stockData = data.optJSONObject(symbol) ?: return KLineData(code, "", period, emptyList())

        val dayKey = when (period) {
            KLinePeriod.DAILY -> "qfqday"
            KLinePeriod.WEEKLY -> "qfqweek"
            KLinePeriod.MONTHLY -> "qfqmonth"
        }
        var klines = stockData.optJSONArray(dayKey)
        if (klines == null) {
            val fallbackKey = when (period) {
                KLinePeriod.DAILY -> "day"
                KLinePeriod.WEEKLY -> "week"
                KLinePeriod.MONTHLY -> "month"
            }
            klines = stockData.optJSONArray(fallbackKey)
        }

        val name = stockData.optJSONObject("qt")?.let { qt ->
            val qtArr = qt.optJSONArray(symbol)
            qtArr?.optString(1, "") ?: ""
        } ?: ""

        val points = mutableListOf<KLinePoint>()
        if (klines != null) {
            for (i in 0 until klines.length()) {
                val row = klines.optJSONArray(i) ?: continue
                if (row.length() < 6) continue
                points.add(
                    KLinePoint(
                        date = row.optString(0, ""),
                        open = row.optString(1, "0").toDoubleOrNull() ?: 0.0,
                        close = row.optString(2, "0").toDoubleOrNull() ?: 0.0,
                        high = row.optString(3, "0").toDoubleOrNull() ?: 0.0,
                        low = row.optString(4, "0").toDoubleOrNull() ?: 0.0,
                        volume = row.optString(5, "0").toDoubleOrNull() ?: 0.0,
                    )
                )
            }
        }

        return KLineData(code = code, name = name, period = period, points = points)
    }

    fun parseTencentHKArray(arr: JSONArray): TencentHKParseResult? {
        if (arr.length() < 38) return null

        val name = arr.optString(1, "")
        val price = arr.optString(3, "0").toDoubleOrNull() ?: 0.0
        val yestClose = arr.optString(4, "0").toDoubleOrNull() ?: 0.0
        val open = arr.optString(5, "0").toDoubleOrNull() ?: 0.0
        val high = arr.optString(33, "0").toDoubleOrNull() ?: 0.0
        val low = arr.optString(34, "0").toDoubleOrNull() ?: 0.0
        val volume = arr.optString(36, "0").toDoubleOrNull() ?: 0.0
        val amount = arr.optString(37, "0").toDoubleOrNull() ?: 0.0
        val time = arr.optString(30, "")

        if (price == 0.0 && yestClose == 0.0) return null

        val actualPrice = if (price == 0.0) yestClose else price
        val changeAmount = actualPrice - yestClose
        val changePercent = if (yestClose > 0) changeAmount / yestClose * 100 else 0.0

        return TencentHKParseResult(
            name = name,
            price = actualPrice,
            yestClose = yestClose,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            time = time,
            changeAmount = changeAmount,
            changePercent = changePercent,
        )
    }

    data class TencentHKParseResult(
        val name: String,
        val price: Double,
        val yestClose: Double,
        val open: Double,
        val high: Double,
        val low: Double,
        val volume: Double,
        val amount: Double,
        val time: String,
        val changeAmount: Double,
        val changePercent: Double,
    )
}