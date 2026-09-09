package com.jiucaihua.app.data.remote.datasource

import com.jiucaihua.app.data.remote.api.TencentFundFlowApi
import com.jiucaihua.app.data.remote.api.TencentSecurityEventApi
import com.jiucaihua.app.domain.model.DataProvider
import com.jiucaihua.app.domain.model.SecurityEvent
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.StockFundFlowPoint
import com.jiucaihua.app.domain.model.StockFundFlowSnapshot
import com.jiucaihua.app.domain.model.UnsupportedSecurityMarketException
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TencentRemoteDataSource @Inject constructor(
    private val eventApi: TencentSecurityEventApi,
    private val fundFlowApi: TencentFundFlowApi,
    private val codeMapper: SecurityCodeMapper,
) {
    suspend fun getEvents(code: SecurityId, kind: SecurityEventKind, page: Int = 1, limit: Int = 20): List<SecurityEvent> {
        val symbol = codeMapper.toNewsSymbol(code).getOrThrow()
        val response = when (kind) {
            SecurityEventKind.NEWS -> eventApi.getNews(symbol, type = 2, page = page)
            SecurityEventKind.ANNOUNCEMENT -> eventApi.getNews(symbol, type = 0, page = page)
            SecurityEventKind.PERIODIC_REPORT -> eventApi.getNotices(symbol, noticeType = PERIODIC_REPORT_TYPE, page = page, limit = limit)
            SecurityEventKind.RESEARCH -> eventApi.getResearch(symbol, page = page, limit = limit)
            SecurityEventKind.INDUSTRY_NEWS -> return emptyList()
        }
        return TencentEventParser.parse(response, kind, code).take(limit)
    }

    suspend fun getStockFundFlow(code: SecurityId): StockFundFlowSnapshot? {
        val symbol = codeMapper.toNewsSymbol(code).getOrThrow()
        if (!symbol.startsWith("sh") && !symbol.startsWith("sz") && !symbol.startsWith("bj")) {
            throw UnsupportedSecurityMarketException(code.value)
        }
        val response = fundFlowApi.getFundFlow("https://proxy.finance.qq.com/cgi/cgi-bin/fundflow/hsfundtab?code=$symbol")
        return TencentFundFlowParser.parse(response, code)
    }

    private companion object {
        const val PERIODIC_REPORT_TYPE = "0103"
    }
}

internal object TencentEventParser {
    private val formatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    )

    fun parse(response: String, kind: SecurityEventKind, fallbackSymbol: SecurityId): List<SecurityEvent> {
        val root = JSONObject(response)
        if (root.optInt("code", -1) != 0) return emptyList()
        val data = root.opt("data")
        val items = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("data") ?: data.optJSONArray("list") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val externalId = item.optString("id").trim()
                if (title.isBlank() || externalId.isBlank()) continue
                val symbols = item.optJSONArray("symbols").toSecurityIds().ifEmpty {
                    listOfNotNull(SecurityId.parse(item.optString("symbol")), fallbackSymbol)
                        .distinct()
                }
                add(
                    SecurityEvent(
                        externalId = externalId,
                        kind = kind,
                        title = title,
                        summary = item.optString("summary").ifBlank { item.optString("desc") }.trim(),
                        contentUrl = item.optString("url").trim(),
                        publisher = item.optString("src").ifBlank { item.optString("source") }.trim(),
                        publishedAt = parseTime(item.optString("time")),
                        symbols = symbols,
                        importance = item.optNullableInt("importance"),
                        titleMention = item.optNullableInt("titleMention"),
                        bodyMention = item.optNullableInt("bodyMention"),
                        researchRating = item.optString("tzpj").trim().ifBlank { null },
                        reportType = item.optString("typeStr").trim().ifBlank { null },
                    )
                )
            }
        }
    }

    private fun JSONArray?.toSecurityIds(): List<SecurityId> = buildList {
        if (this@toSecurityIds == null) return@buildList
        for (index in 0 until this@toSecurityIds.length()) {
            SecurityId.parse(this@toSecurityIds.optString(index))?.let(::add)
        }
    }

    private fun JSONObject.optNullableInt(name: String): Int? = takeIf { has(name) && !isNull(name) }?.optInt(name)

    private fun parseTime(value: String): Long {
        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(value, formatter).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}

internal object TencentFundFlowParser {
    fun parse(response: String, code: SecurityId): StockFundFlowSnapshot? {
        val root = JSONObject(response)
        if (root.optInt("code", -1) != 0) return null
        val data = root.optJSONObject("data") ?: return null
        val today = data.optJSONObject("todayFundFlow") ?: return null
        val trend = data.optJSONObject("todayFundTrend")?.optJSONArray("minList")
        val fiveDay = data.opt("fiveDayFundFlow").asArray()
        return StockFundFlowSnapshot(
            code = code,
            mainNet = today.optDoubleOrNull("mainNetIn"),
            retailNet = today.optDoubleOrNull("retailIn")?.let { retailIn ->
                val retailOut = today.optDoubleOrNull("retailOut") ?: return@let null
                retailIn - retailOut
            },
            superLargeNet = today.optDoubleOrNull("superFlow"),
            largeNet = today.optDoubleOrNull("bigFlow"),
            mediumNet = today.optDoubleOrNull("normalFlow"),
            smallNet = today.optDoubleOrNull("smallFlow"),
            mainNetPercent = today.optDoubleOrNull("mainInRate")?.let { input ->
                val output = today.optDoubleOrNull("mainOutRate") ?: return@let null
                input - output
            },
            retailNetPercent = today.optDoubleOrNull("retailInRate")?.let { input ->
                val output = today.optDoubleOrNull("retailOutRate") ?: return@let null
                input - output
            },
            intradayPoints = trend.toPoints(),
            fiveDaySummary = fiveDay.toPoints(),
            provider = DataProvider.TENCENT,
            warnings = listOf("SOURCE_TIME_UNKNOWN"),
        )
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optString(name).toDoubleOrNull()
    }

    private fun Any?.asArray(): JSONArray? = this as? JSONArray

    private fun JSONArray?.toPoints(): List<StockFundFlowPoint> = buildList {
        if (this@toPoints == null) return@buildList
        for (index in 0 until this@toPoints.length()) {
            val point = this@toPoints.optJSONObject(index) ?: continue
            add(
                StockFundFlowPoint(
                    time = point.optString("time").ifBlank { point.optString("date") },
                    mainNet = point.optDoubleOrNull("MainNetInflow") ?: point.optDoubleOrNull("mainNetIn"),
                    retailNet = point.optDoubleOrNull("RetailNetInflow") ?: point.optDoubleOrNull("retailNetIn"),
                )
            )
        }
    }
}
