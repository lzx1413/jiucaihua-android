package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.entity.StockCacheEntity
import com.jiucaihua.app.data.parser.StockDataParser
import com.jiucaihua.app.data.remote.api.SinaStockApi
import com.jiucaihua.app.data.remote.api.TencentHKStockApi
import com.jiucaihua.app.data.remote.api.TencentKLineApi
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.repository.StockRepository
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
        val symbol = StockDataParser.toTencentKLineSymbol(code)
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
        return StockDataParser.parseTencentKLineResponse(code, symbol, period, response)
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

    private fun parseTencentHKItem(code: String, arr: org.json.JSONArray): StockQuote? {
        val parsed = StockDataParser.parseTencentHKArray(arr)
        if (parsed == null) return null

        return StockQuote(
            code = code,
            name = parsed.name,
            price = parsed.price,
            yestClose = parsed.yestClose,
            open = parsed.open,
            high = parsed.high,
            low = parsed.low,
            volume = parsed.volume,
            amount = parsed.amount,
            changePercent = parsed.changePercent,
            changeAmount = parsed.changeAmount,
            time = parsed.time,
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
