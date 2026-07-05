package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.parser.StockDataParser
import com.jiucaihua.app.data.remote.api.EastMoneyFundFlowApi
import com.jiucaihua.app.data.remote.api.SinaGoldKLineApi
import com.jiucaihua.app.data.remote.api.SinaStockApi
import com.jiucaihua.app.data.remote.api.TencentHKStockApi
import com.jiucaihua.app.data.remote.api.TencentKLineApi
import com.jiucaihua.app.domain.model.FundFlowData
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import com.jiucaihua.app.domain.model.MarketIndex
import com.jiucaihua.app.domain.model.MarketIndexCodes
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NorthFlowData
import com.jiucaihua.app.domain.model.SouthFlowData
import com.jiucaihua.app.domain.repository.MarketRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val sinaStockApi: SinaStockApi,
    private val sinaGoldKLineApi: SinaGoldKLineApi,
    private val tencentHKStockApi: TencentHKStockApi,
    private val tencentKLineApi: TencentKLineApi,
    private val eastMoneyFundFlowApi: EastMoneyFundFlowApi,
) : MarketRepository {

    override suspend fun getAStockIndices(): List<MarketIndex> {
        val codes = MarketIndexCodes.A_STOCK_INDICES
        val url = "https://hq.sinajs.cn/list=${codes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        return parseSinaAStockIndices(response, codes)
    }

    override suspend fun getHKStockIndices(): List<MarketIndex> {
        val codes = MarketIndexCodes.HK_STOCK_INDICES
        val rCodes = codes.joinToString(",") { "r_$it" }
        val url = "https://qt.gtimg.cn/q=$rCodes&fmt=json"
        val response = tencentHKStockApi.getHKStockQuotes(url)
        return parseTencentHKIndices(response, codes)
    }

    override suspend fun getUSStockIndices(): List<MarketIndex> {
        val codes = MarketIndexCodes.US_STOCK_INDICES
        val sinaCodes = codes.map { it.replace("usr_", "gb_") }
        val url = "https://hq.sinajs.cn/list=${sinaCodes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        return parseSinaUSStockIndices(response, codes)
    }

    override suspend fun getFundFlowData(): FundFlowData {
        val url = "https://push2.eastmoney.com/api/qt/kamt/get?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f63&ut=b2884a393a59ad64002292a3e90d46a5"
        val response = eastMoneyFundFlowApi.getFundFlowData(url)
        return parseEastMoneyFundFlow(response)
    }

    override suspend fun getGoldIndices(): List<MarketIndex> {
        val codes = MarketIndexCodes.GOLD_INDICES
        val url = "https://hq.sinajs.cn/list=${codes.joinToString(",")}"
        val response = sinaStockApi.getStockQuotes(url)
        return parseSinaGoldIndices(response, codes)
    }

    override suspend fun getIndexKLineData(code: String, period: KLinePeriod, limit: Int): KLineData {
        if (code.startsWith("hf_") || code.startsWith("gds_")) {
            return getGoldKLineData(code, period)
        }
        if (code.startsWith("usr_")) {
            return getUSStockKLineData(code, period, limit)
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
        val symbol = MarketIndexCodes.GOLD_KLINE_SYMBOLS[code]
        if (symbol == null) {
            return KLineData(code, name, period, emptyList())
        }
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

    private fun parseSinaAStockIndices(response: String, codes: List<String>): List<MarketIndex> {
        val results = mutableListOf<MarketIndex>()
        val codesSet = codes.toSet()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val index = parseSinaAStockLine(line)
                if (index != null && codesSet.contains(index.code)) {
                    results.add(index)
                }
            } catch (_: Exception) {
                continue
            }
        }

        for (code in codes) {
            if (results.none { it.code == code }) {
                results.add(createPlaceholderIndex(code, MarketType.A_STOCK, MarketIndexCodes.A_STOCK_NAMES[code]))
            }
        }

        return results.sortedBy { codes.indexOf(it.code) }
    }

    private fun parseSinaAStockLine(line: String): MarketIndex? {
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

        val changeAmount = price - yestClose
        val changePercent = if (yestClose > 0) changeAmount / yestClose * 100 else 0.0

        return MarketIndex(
            code = codePart,
            name = name,
            price = price,
            yestClose = yestClose,
            changePercent = changePercent,
            changeAmount = changeAmount,
            open = open,
            high = high,
            low = low,
            volume = volume,
            amount = amount,
            time = "$date $time",
            marketType = MarketType.A_STOCK,
        )
    }

    private fun parseTencentHKIndices(response: String, codes: List<String>): List<MarketIndex> {
        val results = mutableListOf<MarketIndex>()
        val codesSet = codes.toSet()
        try {
            val json = JSONObject(response)
            for (code in codes) {
                try {
                    val rCode = "r_$code"
                    val arr = json.optJSONArray(rCode)
                    val parsed = arr?.let { StockDataParser.parseTencentHKArray(it) }
                    if (parsed != null) {
                        results.add(MarketIndex(
                            code = code,
                            name = parsed.name,
                            price = parsed.price,
                            yestClose = parsed.yestClose,
                            changePercent = parsed.changePercent,
                            changeAmount = parsed.changeAmount,
                            open = parsed.open,
                            high = parsed.high,
                            low = parsed.low,
                            volume = parsed.volume,
                            amount = parsed.amount,
                            time = parsed.time,
                            marketType = MarketType.HK_STOCK,
                        ))
                    } else {
                        results.add(createPlaceholderIndex(code, MarketType.HK_STOCK, MarketIndexCodes.HK_STOCK_NAMES[code]))
                    }
                } catch (_: Exception) {
                    results.add(createPlaceholderIndex(code, MarketType.HK_STOCK, MarketIndexCodes.HK_STOCK_NAMES[code]))
                }
            }
        } catch (_: Exception) {
            for (code in codes) {
                results.add(createPlaceholderIndex(code, MarketType.HK_STOCK, MarketIndexCodes.HK_STOCK_NAMES[code]))
            }
        }
        return results.sortedBy { codes.indexOf(it.code) }
    }

    private fun createPlaceholderIndex(code: String, marketType: MarketType, name: String?): MarketIndex {
        return MarketIndex(
            code = code,
            name = name ?: code,
            price = 0.0,
            yestClose = 0.0,
            changePercent = 0.0,
            changeAmount = 0.0,
            marketType = marketType,
        )
    }

    private fun parseSinaUSStockIndices(response: String, originalCodes: List<String>): List<MarketIndex> {
        val results = mutableListOf<MarketIndex>()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        val codeMapping = originalCodes.associateBy { it.replace("usr_", "gb_") }

        for (line in lines) {
            try {
                val gbCode = line.substringAfter("var hq_str_", "").substringBefore("=", "")
                val originalCode = codeMapping[gbCode]
                if (originalCode == null) continue

                val index = parseSinaUSStockLine(line, originalCode)
                if (index != null) {
                    results.add(index)
                }
            } catch (_: Exception) {
                continue
            }
        }

        for (code in originalCodes) {
            if (results.none { it.code == code }) {
                results.add(createPlaceholderIndex(code, MarketType.US_STOCK, MarketIndexCodes.US_STOCK_NAMES[code]))
            }
        }

        return results.sortedBy { originalCodes.indexOf(it.code) }
    }

    private fun parseSinaUSStockLine(line: String, originalCode: String): MarketIndex? {
        val dataPart = line.substringAfter("=\"", "").trimEnd('"')
        if (dataPart.isBlank()) return null

        val params = dataPart.split(",")
        if (params.size < 10) return null

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

        return MarketIndex(
            code = originalCode,
            name = name,
            price = price,
            yestClose = yestClose,
            changePercent = changePercent,
            changeAmount = changeAmount,
            open = open,
            high = high,
            low = low,
            volume = 0.0,
            amount = 0.0,
            time = time,
            marketType = MarketType.US_STOCK,
        )
    }

    private fun parseEastMoneyFundFlow(response: String): FundFlowData {
        try {
            val json = JSONObject(response)
            val data = json.optJSONObject("data") ?: return FundFlowData()

            val hk2sh = data.optJSONObject("hk2sh") ?: JSONObject()
            val sh2hk = data.optJSONObject("sh2hk") ?: JSONObject()
            val hk2sz = data.optJSONObject("hk2sz") ?: JSONObject()
            val sz2hk = data.optJSONObject("sz2hk") ?: JSONObject()

            val hgtNetInflow = hk2sh.optDouble("dayNetAmtIn", 0.0)
            val hgtNetBuy = hk2sh.optDouble("netBuyAmt", 0.0)
            val hgtRemain = hk2sh.optDouble("dayAmtRemain", 0.0)

            val sgtNetInflow = hk2sz.optDouble("dayNetAmtIn", 0.0)
            val sgtNetBuy = hk2sz.optDouble("netBuyAmt", 0.0)
            val sgtRemain = hk2sz.optDouble("dayAmtRemain", 0.0)

            val ggtShNetBuy = sh2hk.optDouble("netBuyAmt", 0.0)
            val ggtShNetInflow = normalizeSouthboundNetInflow(sh2hk, ggtShNetBuy)
            val ggtShRemain = sh2hk.optDouble("dayAmtRemain", 0.0)

            val ggtSzNetBuy = sz2hk.optDouble("netBuyAmt", 0.0)
            val ggtSzNetInflow = normalizeSouthboundNetInflow(sz2hk, ggtSzNetBuy)
            val ggtSzRemain = sz2hk.optDouble("dayAmtRemain", 0.0)

            return FundFlowData(
                updateTime = "",
                northFlow = NorthFlowData(
                    hgtNetInflow = hgtNetInflow,
                    hgtNetBuy = hgtNetBuy,
                    hgtRemain = hgtRemain,
                    sgtNetInflow = sgtNetInflow,
                    sgtNetBuy = sgtNetBuy,
                    sgtRemain = sgtRemain,
                    totalNetInflow = hgtNetInflow + sgtNetInflow,
                ),
                southFlow = SouthFlowData(
                    ggtShNetInflow = ggtShNetInflow,
                    ggtShNetBuy = ggtShNetBuy,
                    ggtShRemain = ggtShRemain,
                    ggtSzNetInflow = ggtSzNetInflow,
                    ggtSzNetBuy = ggtSzNetBuy,
                    ggtSzRemain = ggtSzRemain,
                    totalNetInflow = ggtShNetInflow + ggtSzNetInflow,
                ),
            )
        } catch (_: Exception) {
            return FundFlowData()
        }
    }

    private fun normalizeSouthboundNetInflow(channel: JSONObject, netBuyAmt: Double): Double {
        return normalizeSouthboundNetInflow(
            dayNetAmtIn = channel.optDouble("dayNetAmtIn", 0.0),
            dayAmtRemain = channel.optDouble("dayAmtRemain", 0.0),
            dayAmtThreshold = channel.optDouble("dayAmtThreshold", 0.0),
            netBuyAmt = netBuyAmt,
        )
    }

    private fun parseSinaGoldIndices(response: String, codes: List<String>): List<MarketIndex> {
        val results = mutableListOf<MarketIndex>()
        val lines = response.split(";\n").filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val index = parseSinaGoldLine(line)
                if (index != null) {
                    results.add(index)
                }
            } catch (_: Exception) {
                continue
            }
        }

        for (code in codes) {
            if (results.none { it.code == code }) {
                results.add(createPlaceholderIndex(code, MarketType.GOLD, MarketIndexCodes.GOLD_NAMES[code]))
            }
        }

        return results.sortedBy { codes.indexOf(it.code) }
    }

    private fun parseSinaGoldLine(line: String): MarketIndex? {
        val codePart = line.substringAfter("var hq_str_", "").substringBefore("=", "")
        if (codePart.isBlank()) return null

        val dataPart = line.substringAfter("=\"", "").trimEnd('"')
        if (dataPart.isBlank()) return null

        val params = dataPart.split(",")
        if (params.size < 14) return null

        val price = params[0].toDoubleOrNull() ?: 0.0
        val high = params[4].toDoubleOrNull() ?: 0.0
        val low = params[5].toDoubleOrNull() ?: 0.0
        val time = params[6]
        val yestClose = params[7].toDoubleOrNull() ?: 0.0
        val open = params[8].toDoubleOrNull() ?: 0.0
        val date = params[12]
        val name = MarketIndexCodes.GOLD_NAMES[codePart] ?: params[13]

        val changeAmount = if (yestClose > 0) price - yestClose else 0.0
        val changePercent = if (yestClose > 0) changeAmount / yestClose * 100 else 0.0

        return MarketIndex(
            code = codePart,
            name = name,
            price = price,
            yestClose = yestClose,
            changePercent = changePercent,
            changeAmount = changeAmount,
            open = open,
            high = high,
            low = low,
            time = "$date $time",
            marketType = MarketType.GOLD,
        )
    }
}

internal fun normalizeSouthboundNetInflow(
    dayNetAmtIn: Double,
    dayAmtRemain: Double,
    dayAmtThreshold: Double,
    netBuyAmt: Double,
): Double {
    return if (dayAmtThreshold > 0.0 && dayAmtRemain == 0.0 && dayNetAmtIn >= dayAmtThreshold) {
        netBuyAmt
    } else {
        dayNetAmtIn
    }
}
