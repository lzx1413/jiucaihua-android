package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.parser.StockDataParser
import com.jiucaihua.app.data.remote.api.EastMoneyFundFlowApi
import com.jiucaihua.app.data.remote.api.EastMoneyUSStockKLineApi
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
    private val tencentHKStockApi: TencentHKStockApi,
    private val tencentKLineApi: TencentKLineApi,
    private val eastMoneyFundFlowApi: EastMoneyFundFlowApi,
    private val eastMoneyUSStockKLineApi: EastMoneyUSStockKLineApi,
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

    override suspend fun getIndexKLineData(code: String, period: KLinePeriod, limit: Int): KLineData {
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
        cal.add(Calendar.YEAR, -1)
        val startDate = dateFormat.format(cal.time)

        val param = "$symbol,$periodType,$startDate,$endDate,$limit,qfq"
        val response = tencentKLineApi.getKLineData(param)
        return StockDataParser.parseTencentKLineResponse(code, symbol, period, response)
    }

    private suspend fun getUSStockKLineData(code: String, period: KLinePeriod, limit: Int): KLineData {
        val secId = when (code) {
            "usr_dji" -> "105.DJI"
            "usr_ixic" -> "105.IXIC"
            "usr_inx" -> "105.INX"
            else -> "105.${code.removePrefix("usr_").uppercase()}"
        }

        val klt = when (period) {
            KLinePeriod.DAILY -> 101
            KLinePeriod.WEEKLY -> 102
            KLinePeriod.MONTHLY -> 103
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = dateFormat.format(cal.time)
        cal.add(Calendar.YEAR, -1)
        val startDate = dateFormat.format(cal.time)

        val url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=$secId&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=$klt&fqt=1&beg=$startDate&end=$endDate&ut=b2884a393a59ad64002292a3e90d46a5"

        val response = eastMoneyUSStockKLineApi.getUSStockKLineData(url)
        return parseEastMoneyUSStockKLineResponse(code, period, response)
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

            val ggtShNetInflow = sh2hk.optDouble("dayNetAmtIn", 0.0)
            val ggtShNetBuy = sh2hk.optDouble("netBuyAmt", 0.0)
            val ggtShRemain = sh2hk.optDouble("dayAmtRemain", 0.0)

            val ggtSzNetInflow = sz2hk.optDouble("dayNetAmtIn", 0.0)
            val ggtSzNetBuy = sz2hk.optDouble("netBuyAmt", 0.0)
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

    private fun parseEastMoneyUSStockKLineResponse(code: String, period: KLinePeriod, response: String): KLineData {
        try {
            val json = JSONObject(response)
            val data = json.optJSONObject("data") ?: return KLineData(code, "", period, emptyList())
            val klines = data.optJSONArray("klines") ?: return KLineData(code, "", period, emptyList())

            val name = MarketIndexCodes.US_STOCK_NAMES[code] ?: ""

            val points = mutableListOf<KLinePoint>()
            for (i in 0 until klines.length()) {
                val rowStr = klines.optString(i, "")
                if (rowStr.isBlank()) continue

                val parts = rowStr.split(",")
                if (parts.size < 6) continue

                points.add(
                    KLinePoint(
                        date = parts[0],
                        open = parts[1].toDoubleOrNull() ?: 0.0,
                        close = parts[2].toDoubleOrNull() ?: 0.0,
                        high = parts[3].toDoubleOrNull() ?: 0.0,
                        low = parts[4].toDoubleOrNull() ?: 0.0,
                        volume = parts[5].toDoubleOrNull() ?: 0.0,
                    )
                )
            }

            return KLineData(code = code, name = name, period = period, points = points)
        } catch (_: Exception) {
            return KLineData(code, "", period, emptyList())
        }
    }
}