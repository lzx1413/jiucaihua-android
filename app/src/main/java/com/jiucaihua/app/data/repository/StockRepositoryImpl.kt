package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.entity.StockCacheEntity
import com.jiucaihua.app.data.parser.StockDataParser
import com.jiucaihua.app.data.remote.api.SinaGoldKLineApi
import com.jiucaihua.app.data.remote.api.SinaStockApi
import com.jiucaihua.app.data.remote.api.TencentHKStockApi
import com.jiucaihua.app.data.remote.api.TencentKLineApi
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import com.jiucaihua.app.domain.model.MarketIndexCodes
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
    private val sinaGoldKLineApi: SinaGoldKLineApi,
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

    override suspend fun getUSStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()

        val usCodes = codes.filter { it.startsWith("usr_") || it.startsWith("gb_") }
        if (usCodes.isEmpty()) return emptyList()

        val sinaCodes = usCodes.map { it.replace("usr_", "gb_") }
        val url = "https://hq.sinajs.cn/list=${sinaCodes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        val codeMapping = usCodes.associateBy { it.replace("usr_", "gb_") }
        val quotes = parseSinaUSResponse(response, codeMapping)

        if (quotes.isNotEmpty()) {
            val cacheEntities = quotes.map { it.toCacheEntity() }
            stockCacheDao.insertAll(cacheEntities)
        }

        return quotes
    }

    override suspend fun getGoldQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()

        val goldCodes = codes.filter { it.startsWith("hf_") || it.startsWith("gds_") }
        if (goldCodes.isEmpty()) return emptyList()

        val url = "https://hq.sinajs.cn/list=${goldCodes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        val quotes = parseSinaGoldResponse(response)

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
        if (code.startsWith("usr_")) {
            return getUSStockKLineData(code, period, limit)
        }
        if (code.startsWith("hf_") || code.startsWith("gds_")) {
            return getGoldKLineData(code, period)
        }

        val symbol = StockDataParser.toTencentKLineSymbol(code)
        val periodType = when (period) {
            KLinePeriod.DAILY -> "day"
            KLinePeriod.WEEKLY -> "week"
            KLinePeriod.MONTHLY -> "month"
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = dateFormat.format(cal.time)
        when (period) {
            KLinePeriod.DAILY -> cal.add(Calendar.YEAR, -1)
            KLinePeriod.WEEKLY -> cal.add(Calendar.YEAR, -5)
            KLinePeriod.MONTHLY -> cal.add(Calendar.YEAR, -10)
        }
        val startDate = dateFormat.format(cal.time)

        val param = "$symbol,$periodType,$startDate,$endDate,$limit,qfq"
        val response = tencentKLineApi.getKLineData(param)
        return StockDataParser.parseTencentKLineResponse(code, symbol, period, response)
    }

    private suspend fun getUSStockKLineData(code: String, period: KLinePeriod, limit: Int): KLineData {
        val symbol = "us.${code.removePrefix("usr_").uppercase()}"
        val periodType = when (period) {
            KLinePeriod.DAILY -> "day"
            KLinePeriod.WEEKLY -> "week"
            KLinePeriod.MONTHLY -> "month"
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = dateFormat.format(cal.time)
        when (period) {
            KLinePeriod.DAILY -> cal.add(Calendar.YEAR, -1)
            KLinePeriod.WEEKLY -> cal.add(Calendar.YEAR, -5)
            KLinePeriod.MONTHLY -> cal.add(Calendar.YEAR, -10)
        }
        val startDate = dateFormat.format(cal.time)

        val param = "$symbol,$periodType,$startDate,$endDate,$limit,qfq"
        val response = tencentKLineApi.getKLineData(param)
        return StockDataParser.parseTencentKLineResponse(code, symbol, period, response)
    }

    private suspend fun getGoldKLineData(code: String, period: KLinePeriod): KLineData {
        val name = MarketIndexCodes.GOLD_NAMES[code] ?: ""
        val symbol = "au0"
        val url = "https://stock2.finance.sina.com.cn/futures/api/jsonp.php/var%20kline=/InnerFuturesNewService.getDailyKLine?symbol=$symbol&_=${System.currentTimeMillis()}"

        return try {
            val response = sinaGoldKLineApi.getGoldKLine(url)
            parseSinaGoldKLineResponse(code, name, period, response)
        } catch (_: Exception) {
            KLineData(code, name, period, emptyList())
        }
    }

    private fun parseSinaGoldKLineResponse(code: String, name: String, period: KLinePeriod, response: String): KLineData {
        try {
            val jsonStart = response.indexOf("([")
            val jsonEnd = response.lastIndexOf("])")
            if (jsonStart < 0 || jsonEnd < 0) return KLineData(code, name, period, emptyList())

            val jsonStr = response.substring(jsonStart + 1, jsonEnd + 1)
            val arr = org.json.JSONArray(jsonStr)

            val points = mutableListOf<KLinePoint>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                points.add(
                    KLinePoint(
                        date = item.optString("d", ""),
                        open = item.optString("o", "0").toDoubleOrNull() ?: 0.0,
                        close = item.optString("c", "0").toDoubleOrNull() ?: 0.0,
                        high = item.optString("h", "0").toDoubleOrNull() ?: 0.0,
                        low = item.optString("l", "0").toDoubleOrNull() ?: 0.0,
                        volume = item.optString("v", "0").toDoubleOrNull() ?: 0.0,
                    )
                )
            }

            val filteredPoints = when (period) {
                KLinePeriod.DAILY -> points.takeLast(120)
                KLinePeriod.WEEKLY -> aggregateToWeekly(points).takeLast(120)
                KLinePeriod.MONTHLY -> aggregateToMonthly(points).takeLast(120)
            }

            return KLineData(code = code, name = name, period = period, points = filteredPoints)
        } catch (_: Exception) {
            return KLineData(code, name, period, emptyList())
        }
    }

    private fun aggregateToWeekly(dailyPoints: List<KLinePoint>): List<KLinePoint> {
        if (dailyPoints.isEmpty()) return emptyList()
        val weeks = mutableListOf<KLinePoint>()
        var weekOpen = 0.0
        var weekHigh = 0.0
        var weekLow = Double.MAX_VALUE
        var weekClose = 0.0
        var weekVolume = 0.0
        var weekDate = ""
        var lastDayOfWeek = -1

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (point in dailyPoints) {
            try { cal.time = dateFormat.parse(point.date) ?: continue } catch (_: Exception) { continue }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (lastDayOfWeek != -1 && dayOfWeek <= lastDayOfWeek && weekDate.isNotEmpty()) {
                weeks.add(KLinePoint(weekDate, weekOpen, weekClose, weekHigh, weekLow, weekVolume))
                weekOpen = 0.0
                weekHigh = 0.0
                weekLow = Double.MAX_VALUE
                weekVolume = 0.0
            }

            if (weekOpen == 0.0) weekOpen = point.open
            weekHigh = maxOf(weekHigh, point.high)
            weekLow = minOf(weekLow, point.low)
            weekClose = point.close
            weekVolume += point.volume
            weekDate = point.date
            lastDayOfWeek = dayOfWeek
        }

        if (weekDate.isNotEmpty()) {
            weeks.add(KLinePoint(weekDate, weekOpen, weekClose, weekHigh, weekLow, weekVolume))
        }

        return weeks
    }

    private fun aggregateToMonthly(dailyPoints: List<KLinePoint>): List<KLinePoint> {
        if (dailyPoints.isEmpty()) return emptyList()
        val months = mutableListOf<KLinePoint>()
        var monthOpen = 0.0
        var monthHigh = 0.0
        var monthLow = Double.MAX_VALUE
        var monthClose = 0.0
        var monthVolume = 0.0
        var monthDate = ""
        var lastMonth = ""

        for (point in dailyPoints) {
            val currentMonth = point.date.substring(0, 7)

            if (lastMonth.isNotEmpty() && currentMonth != lastMonth && monthDate.isNotEmpty()) {
                months.add(KLinePoint(monthDate, monthOpen, monthClose, monthHigh, monthLow, monthVolume))
                monthOpen = 0.0
                monthHigh = 0.0
                monthLow = Double.MAX_VALUE
                monthVolume = 0.0
            }

            if (monthOpen == 0.0) monthOpen = point.open
            monthHigh = maxOf(monthHigh, point.high)
            monthLow = minOf(monthLow, point.low)
            monthClose = point.close
            monthVolume += point.volume
            monthDate = point.date
            lastMonth = currentMonth
        }

        if (monthDate.isNotEmpty()) {
            months.add(KLinePoint(monthDate, monthOpen, monthClose, monthHigh, monthLow, monthVolume))
        }

        return months
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

    private fun parseSinaUSResponse(response: String, codeMapping: Map<String, String>): List<StockQuote> {
        val results = mutableListOf<StockQuote>()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val gbCode = line.substringAfter("var hq_str_", "").substringBefore("=", "")
                val originalCode = codeMapping[gbCode] ?: continue

                val dataPart = line.substringAfter("=\"", "").trimEnd('"')
                if (dataPart.isBlank()) continue

                val params = dataPart.split(",")
                if (params.size < 8) continue

                val name = params[0]
                val price = params[1].toDoubleOrNull() ?: 0.0
                val changePercent = params[2].toDoubleOrNull() ?: 0.0
                val time = if (params.size > 3) params[3] else ""
                val changeAmount = if (params.size > 4) params[4].toDoubleOrNull() ?: 0.0 else 0.0
                val open = if (params.size > 5) params[5].toDoubleOrNull() ?: 0.0 else 0.0
                val high = if (params.size > 6) params[6].toDoubleOrNull() ?: 0.0 else 0.0
                val low = if (params.size > 7) params[7].toDoubleOrNull() ?: 0.0 else 0.0

                val yestClose = if (price != 0.0 && changeAmount != 0.0) {
                    price - changeAmount
                } else if (price != 0.0 && changePercent != 0.0) {
                    price / (1 + changePercent / 100)
                } else {
                    0.0
                }

                if (price == 0.0) continue

                results.add(StockQuote(
                    code = originalCode,
                    name = name.ifBlank { MarketIndexCodes.US_STOCK_NAMES[originalCode] ?: originalCode },
                    price = price,
                    yestClose = yestClose,
                    open = open,
                    high = high,
                    low = low,
                    volume = 0.0,
                    amount = 0.0,
                    changePercent = changePercent,
                    changeAmount = changeAmount,
                    time = time,
                    marketType = MarketType.US_STOCK,
                ))
            } catch (_: Exception) {
                continue
            }
        }

        return results
    }

    private fun parseSinaGoldResponse(response: String): List<StockQuote> {
        val results = mutableListOf<StockQuote>()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val codePart = line.substringAfter("var hq_str_", "").substringBefore("=", "")
                if (codePart.isBlank()) continue

                val dataPart = line.substringAfter("=\"", "").trimEnd('"')
                if (dataPart.isBlank()) continue

                val params = dataPart.split(",")
                if (params.size < 14) continue

                val price = params[0].toDoubleOrNull() ?: 0.0
                val high = params[4].toDoubleOrNull() ?: 0.0
                val low = params[5].toDoubleOrNull() ?: 0.0
                val time = params[6]
                val yestClose = params[7].toDoubleOrNull() ?: 0.0
                val open = params[8].toDoubleOrNull() ?: 0.0
                val date = params[12]
                val name = MarketIndexCodes.GOLD_NAMES[codePart] ?: params[13]

                if (price == 0.0) continue

                val changeAmount = if (yestClose > 0) price - yestClose else 0.0
                val changePercent = if (yestClose > 0) changeAmount / yestClose * 100 else 0.0

                results.add(StockQuote(
                    code = codePart,
                    name = name,
                    price = price,
                    yestClose = yestClose,
                    open = open,
                    high = high,
                    low = low,
                    volume = 0.0,
                    amount = 0.0,
                    changePercent = changePercent,
                    changeAmount = changeAmount,
                    time = "$date $time",
                    marketType = MarketType.GOLD,
                ))
            } catch (_: Exception) {
                continue
            }
        }

        return results
    }

    private fun StockQuote.toCacheEntity(): StockCacheEntity {
        return StockCacheEntity(
            code = code,
            name = name,
            currency = when (marketType) {
                MarketType.HK_STOCK -> "HKD"
                MarketType.US_STOCK -> "USD"
                else -> "CNY"
            },
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
