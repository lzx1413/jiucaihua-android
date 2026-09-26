package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.SecurityEvent
import com.jiucaihua.app.domain.model.StockFundFlowSnapshot

data class SecurityEventToolSnapshot(
    val externalId: String,
    val kind: String,
    val title: String,
    val summary: String,
    val publisher: String,
    val publishedAt: Long?,
    val symbols: List<String>,
    val url: String,
    val provider: String,
    val isStale: Boolean,
    val attributes: Map<String, String?> = emptyMap(),
)

data class StockEventsToolSnapshot(
    val code: String,
    val items: List<SecurityEventToolSnapshot>,
    val warnings: List<String> = emptyList(),
    val limit: Int,
    val possiblyTruncated: Boolean,
)

data class StockFundFlowToolSnapshot(
    val code: String,
    val period: String,
    val currency: String,
    val unit: String,
    val sourceUpdatedAt: Long?,
    val mainNet: Double?,
    val retailNet: Double?,
    val superLargeNet: Double?,
    val largeNet: Double?,
    val mediumNet: Double?,
    val smallNet: Double?,
    val mainNetPercent: Double?,
    val retailNetPercent: Double?,
    val intradayPoints: List<Map<String, Any?>>? = null,
    val fiveDaySummary: List<Map<String, Any?>>? = null,
    val provider: String,
    val isStale: Boolean,
    val warnings: List<String>,
)

fun SecurityEvent.toToolSnapshot() = SecurityEventToolSnapshot(
    externalId = externalId,
    kind = kind.name,
    title = title,
    summary = summary,
    publisher = publisher,
    publishedAt = publishedAt.takeIf { it > 0 },
    symbols = symbols.map { it.value },
    url = contentUrl,
    provider = provider.name,
    isStale = isStale,
    attributes = mapOf("researchRating" to researchRating, "reportType" to reportType),
)

fun StockFundFlowSnapshot.toToolSnapshot(period: String = "summary") = StockFundFlowToolSnapshot(
    code = code.value,
    period = period,
    currency = currency,
    unit = unit,
    sourceUpdatedAt = sourceUpdatedAt,
    mainNet = mainNet,
    retailNet = retailNet,
    superLargeNet = superLargeNet,
    largeNet = largeNet,
    mediumNet = mediumNet,
    smallNet = smallNet,
    mainNetPercent = mainNetPercent,
    retailNetPercent = retailNetPercent,
    intradayPoints = if (period == "5d" || period == "summary") null else intradayPoints.map { mapOf("time" to it.time, "mainNet" to it.mainNet, "retailNet" to it.retailNet) },
    fiveDaySummary = if (period == "intraday" || period == "summary") null else fiveDaySummary.map { mapOf("time" to it.time, "mainNet" to it.mainNet, "retailNet" to it.retailNet) },
    provider = provider.name,
    isStale = isStale,
    warnings = warnings,
)
