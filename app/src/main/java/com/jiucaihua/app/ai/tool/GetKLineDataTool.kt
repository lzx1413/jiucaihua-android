package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildKLineToolSnapshotUseCase
import com.jiucaihua.app.domain.model.KLinePeriod
import javax.inject.Inject

class GetKLineDataTool @Inject constructor(
    private val buildKLineToolSnapshotUseCase: BuildKLineToolSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_kline_data",
        description = "获取K线序列及OHLCV、均线、量比、MACD、RSI、布林带。数值最多3位小数；CETP省略缺失指标。只需技术摘要时优先用get_indicator_snapshot。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "证券或基金代码，例如 sh600519、hk00700、110011",
                ),
                "period" to mapOf(
                    "type" to "string",
                    "enum" to listOf("DAILY", "WEEKLY", "MONTHLY"),
                    "description" to "K线周期，默认 DAILY",
                ),
                "limit" to mapOf(
                    "type" to "integer",
                    "minimum" to 1,
                    "maximum" to 120,
                    "description" to "返回的点位数量，默认 60，最大 120",
                ),
                "include_indicators" to mapOf("type" to "boolean", "description" to "默认true；false仅返回OHLCV及涨跌幅"),
                "include_latest" to mapOf(
                    "type" to "boolean",
                    "description" to "是否额外返回 latestPoint；默认 false，因为它与points最后一项重复",
                ),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = normalizedQuoteCode(arguments["code"])
        val period = parsePeriod(arguments["period"] as? String)
        val limit = (arguments["limit"] as? Number)?.toInt()?.coerceIn(1, 120) ?: 60
        if (com.jiucaihua.app.domain.model.MarketType.fromCode(code) == com.jiucaihua.app.domain.model.MarketType.FUND && period != KLinePeriod.DAILY) unsupportedMarket("fund NAV history supports DAILY only")
        val includeIndicators = arguments["include_indicators"] as? Boolean ?: true
        val includeLatest = arguments["include_latest"] as? Boolean ?: false
        return ToolResult(buildKLineToolSnapshotUseCase(code, period, limit, includeLatest, includeIndicators))
    }

    private fun parsePeriod(value: String?): KLinePeriod {
        return value?.trim()?.uppercase()?.let {
            runCatching { KLinePeriod.valueOf(it) }.getOrNull()
                ?: invalidArgs("period must be DAILY, WEEKLY, or MONTHLY")
        } ?: KLinePeriod.DAILY
    }
}
