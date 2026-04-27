package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.entity.StockCacheEntity
import com.jiucaihua.app.data.remote.api.SinaStockApi
import com.jiucaihua.app.data.remote.api.TencentHKStockApi
import com.jiucaihua.app.data.remote.api.TencentKLineApi
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.repository.StockRepository
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val sinaStockApi: SinaStockApi,
    private val tencentHKStockApi: TencentHKStockApi,
    private val tencentKLineApi: TencentKLineApi,
    private val stockCacheDao: StockCacheDao,
) : StockRepository {

    override suspend fun getAStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()

        val aStockCodes = codes.filter { it.startsWith("sh") || it.startsWith("sz") || it.startsWith("bj") }
        if (aStockCodes.isEmpty()) return emptyList()

        val url = "https://hq.sinajs.cn/list=${aStockCodes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        val quotes = parseSinaResponse(response)

        if (quotes.isNotEmpty()) {
            val cacheEntities = quotes.map { it.toCacheEntity() }
            stockCacheDao.insertAll(cacheEntities)
        }

        return quotes
    }

    override suspend fun getHKStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()

        val hkCodes = codes.filter { it.startsWith("hk") }
        if (hkCodes.isEmpty()) return emptyList()

        val rCodes = hkCodes.joinToString(",") { "r_$it" }
        val url = "https://qt.gtimg.cn/q=$rCodes&fmt=json"
        val response = tencentHKStockApi.getHKStockQuotes(url)
        val quotes = parseTencentHKResponse(response, hkCodes)

        if (quotes.isNotEmpty()) {
            val cacheEntities = quotes.map { it.toCacheEntity() }
            stockCacheDao.insertAll(cacheEntities)
        }

        return quotes
    }

    override suspend fun getCachedQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()
        return stockCacheDao.getByCodes(codes).map { it.toDomain() }
    }

    override suspend fun getKLineData(code: String, period: KLinePeriod, limit: Int): KLineData {
        val symbol = toTencentKLineSymbol(code)
        val periodType = when (period) {
            KLinePeriod.DAILY -> "day"
            KLinePeriod.WEEKLY -> "week"
            KLinePeriod.MONTHLY -> "month"
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = dateFormat.format(cal.time)
        cal.add(Calendar.YEAR, -1)
        val startDate = dateFormat.format(cal.time)

        val param = "$symbol,$periodType,$startDate,$endDate,$limit,qfq"
        val response = tencentKLineApi.getKLineData(param)
        return parseTencentKLineResponse(code, symbol, period, response)
    }

    private fun toTencentKLineSymbol(code: String): String {
        return when {
            code.startsWith("hk") -> {
                val suffix = code.removePrefix("hk")
                if (suffix.all { it.isDigit() }) "hk${suffix.padStart(5, '0')}" else "hk${suffix.uppercase()}"
            }
            else -> code
        }
    }

    private fun parseTencentKLineResponse(code: String, symbol: String, period: KLinePeriod, response: String): KLineData {
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

    private fun parseTencentHKResponse(response: String, codes: List<String>): List<StockQuote> {
        val results = mutableListOf<StockQuote>()
        try {
            val json = JSONObject(response)
            for (code in codes) {
                try {
                    val rCode = "r_$code"
                    val arr = json.optJSONArray(rCode) ?: continue
                    val quote = parseTencentHKItem(code, arr) ?: continue
                    results.add(quote)
                } catch (_: Exception) {
                    continue
                }
            }
        } catch (_: Exception) {
        }
        return results
    }

    private fun parseTencentHKItem(code: String, arr: JSONArray): StockQuote? {
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

        return StockQuote(
            code = code,
            name = name,
            price = actualPrice,
            yestClose = yestClose,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            changePercent = changePercent,
            changeAmount = changeAmount,
            time = time,
            marketType = MarketType.HK_STOCK,
        )
    }

    private fun parseSinaResponse(response: String): List<StockQuote> {
        val results = mutableListOf<StockQuote>()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val quote = parseSinaLine(line) ?: continue
                results.add(quote)
            } catch (_: Exception) {
                continue
            }
        }

        return results
    }

    private fun parseSinaLine(line: String): StockQuote? {
        val codePart = line.substringAfter("var hq_str_", "").substringBefore("=", "")
        if (codePart.isBlank()) return null

        val dataPart = line.substringAfter("=\"", "").trimEnd('"')
        if (dataPart.isBlank()) return null

        val params = dataPart.split(",")
        if (params.size < 32) return null

        val name = params[0]
        val open = params[1].toDoubleOrNull() ?: 0.0
        val yestClose = params[2].toDoubleOrNull() ?: 0.0
        var price = params[3].toDoubleOrNull() ?: 0.0
        val high = params[4].toDoubleOrNull() ?: 0.0
        val low = params[5].toDoubleOrNull() ?: 0.0
        val volume = params[8].toDoubleOrNull() ?: 0.0
        val amount = params[9].toDoubleOrNull() ?: 0.0
        val date = params[30]
        val time = params[31]

        if (price == 0.0) {
            val buy1 = params[6].toDoubleOrNull() ?: 0.0
            price = if (buy1 != 0.0) buy1 else yestClose
        }

        if (price == 0.0 && high == 0.0 && low == 0.0 && yestClose == 0.0) {
            return null
        }

        val changeAmount = price - yestClose
        val changePercent = if (yestClose > 0) changeAmount / yestClose * 100 else 0.0

        return StockQuote(
            code = codePart,
            name = name,
            price = price,
            yestClose = yestClose,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            changePercent = changePercent,
            changeAmount = changeAmount,
            time = "$date $time",
            marketType = MarketType.A_STOCK,
        )
    }

    private fun StockQuote.toCacheEntity(): StockCacheEntity {
        return StockCacheEntity(
            code = code,
            name = name,
            currency = if (marketType == MarketType.HK_STOCK) "HKD" else "CNY",
            price = price,
            yestClose = yestClose,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            changePercent = changePercent,
            changeAmount = changeAmount,
            time = time,
            marketType = marketType.name,
        )
    }

    private fun StockCacheEntity.toDomain(): StockQuote {
        return StockQuote(
            code = code,
            name = name,
            price = price,
            yestClose = yestClose,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            changePercent = changePercent,
            changeAmount = changeAmount,
            time = time,
            marketType = MarketType.valueOf(marketType),
        )
    }
}
