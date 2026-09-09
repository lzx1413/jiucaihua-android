package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.repository.SecurityEventRepository
import com.jiucaihua.app.domain.repository.SecurityInsightRepository
import com.jiucaihua.app.domain.repository.StockRepository
import java.time.Instant
import javax.inject.Inject

data class StockContextToolSnapshot(
    val code: String,
    val name: String,
    val generatedAt: String,
    val quote: Map<String, Any?>,
    val technical: Map<String, Any?>,
    val events: Map<String, Any?>,
    val stockFundFlow: Map<String, Any?>,
    val profile: Map<String, Any?>,
    val warnings: List<String>,
)

class GetStockContextTool @Inject constructor(
    private val stockRepository: StockRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val securityInsightRepository: SecurityInsightRepository,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_stock_context",
        description = "一次获取个股行情、K线技术摘要、精确关联事件及个股资金流。每个区块独立报告状态，部分失败不会掩盖其余数据。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string"),
                "sections" to mapOf("type" to "array", "items" to mapOf("type" to "string", "enum" to listOf("quote", "technical", "events", "stockFundFlow", "profile"))),
                "event_limit" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 50),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = GetStockEventsTool.parseSecurityId(arguments["code"])
        val sections = (arguments["sections"] as? List<*>)?.map {
            it as? String ?: invalidArgs("sections must be an array of strings")
        }?.toSet() ?: setOf("quote", "technical", "events", "stockFundFlow", "profile")
        if (!sections.all { it in VALID_SECTIONS }) invalidArgs("sections contains an unsupported value")
        val eventLimit = (arguments["event_limit"] as? Number)?.toInt()?.coerceIn(1, 50) ?: 10
        val warnings = mutableListOf<String>()
        val quote = if ("quote" in sections) quoteSection(code.value, code.marketType, warnings) else unavailable("NOT_REQUESTED")
        val technical = if ("technical" in sections) technicalSection(code.value, warnings) else unavailable("NOT_REQUESTED")
        val events = if ("events" in sections) eventsSection(code, eventLimit, warnings) else unavailable("NOT_REQUESTED")
        val flow = if ("stockFundFlow" in sections) flowSection(code, warnings) else unavailable("NOT_REQUESTED")
        return ToolResult(
            StockContextToolSnapshot(
                code = code.value,
                name = quote["name"] as? String ?: "",
                generatedAt = Instant.now().toString(),
                quote = quote,
                technical = technical,
                events = events,
                stockFundFlow = flow,
                profile = if ("profile" in sections) profileSection(code, warnings) else unavailable("NOT_REQUESTED"),
                warnings = warnings,
            )
        )
    }

    private suspend fun quoteSection(code: String, market: MarketType, warnings: MutableList<String>): Map<String, Any?> {
        return try {
            val quote = when (market) {
                MarketType.A_STOCK -> stockRepository.getAStockQuotes(listOf(code)).firstOrNull()
                MarketType.HK_STOCK -> stockRepository.getHKStockQuotes(listOf(code)).firstOrNull()
                MarketType.US_STOCK -> stockRepository.getUSStockQuotes(listOf(code)).firstOrNull()
                else -> null
            }
            if (quote == null) unavailable("UNAVAILABLE") else mapOf(
                "status" to "ok",
                "provider" to if (market == MarketType.HK_STOCK) "TENCENT" else "SINA",
                "sourceUpdatedAt" to quote.time,
                "name" to quote.name,
                "price" to quote.price,
                "changePercent" to quote.changePercent,
            )
        } catch (_: Exception) {
            warnings += "QUOTE_UNAVAILABLE"
            unavailable("UNAVAILABLE")
        }
    }

    private suspend fun technicalSection(code: String, warnings: MutableList<String>): Map<String, Any?> {
        return try {
            val points = stockRepository.getKLineData(code, KLinePeriod.DAILY, 60).points.takeLast(60)
            if (points.size < 20) unavailable("INSUFFICIENT_DATA") else {
                val first = points.first().close
                val latest = points.last().close
                mapOf("status" to "ok", "period" to "DAILY", "summary" to mapOf("pointCount" to points.size, "latestClose" to latest, "rangeLow" to points.minOf { it.low }, "rangeHigh" to points.maxOf { it.high }, "changePercent" to if (first == 0.0) null else (latest - first) / first * 100))
            }
        } catch (_: Exception) {
            warnings += "TECHNICAL_UNAVAILABLE"
            unavailable("UNAVAILABLE")
        }
    }

    private suspend fun eventsSection(code: com.jiucaihua.app.domain.model.SecurityId, limit: Int, warnings: MutableList<String>): Map<String, Any?> = try {
        val events = securityEventRepository.getEvents(code, setOf(SecurityEventKind.NEWS, SecurityEventKind.ANNOUNCEMENT, SecurityEventKind.PERIODIC_REPORT, SecurityEventKind.RESEARCH), limit)
        mapOf("status" to "ok", "items" to events.map { it.toToolSnapshot() })
    } catch (_: Exception) {
        warnings += "EVENTS_UNAVAILABLE"
        unavailable("UNAVAILABLE")
    }

    private suspend fun flowSection(code: com.jiucaihua.app.domain.model.SecurityId, warnings: MutableList<String>): Map<String, Any?> = try {
        securityEventRepository.getStockFundFlow(code)?.toToolSnapshot()?.let { mapOf("status" to "ok", "data" to it) } ?: unavailable("UNAVAILABLE")
    } catch (_: Exception) {
        warnings += "STOCK_FUND_FLOW_UNAVAILABLE"
        unavailable("UNAVAILABLE")
    }

    private suspend fun profileSection(code: com.jiucaihua.app.domain.model.SecurityId, warnings: MutableList<String>): Map<String, Any?> = try {
        val relations = securityInsightRepository.getRelations(code)
        mapOf(
            "status" to "ok",
            "provider" to relations.provider.name,
            "fetchedAt" to relations.fetchedAt,
            "plates" to relations.plates.map { mapOf("code" to it.code, "name" to it.name) },
            "relatedSecurities" to relations.relatedSecurities.map { mapOf("code" to it.code, "name" to it.name) },
            "shareholderReportPeriod" to relations.shareholderReportPeriod,
            "shareholders" to relations.shareholders.map { shareholder ->
                mapOf("name" to shareholder.name, "holdingShares" to shareholder.holdingShares, "floatPercent" to shareholder.floatPercent, "previousHoldingShares" to shareholder.previousHoldingShares, "shareNature" to shareholder.shareNature)
            },
            "warnings" to relations.warnings,
        )
    } catch (_: Exception) {
        warnings += "PROFILE_UNAVAILABLE"
        unavailable("UNAVAILABLE")
    }

    private fun unavailable(reason: String) = mapOf<String, Any?>("status" to "unavailable", "reason" to reason)

    private companion object {
        val VALID_SECTIONS = setOf("quote", "technical", "events", "stockFundFlow", "profile")
    }
}
