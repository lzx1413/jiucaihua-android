package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildKLineToolSnapshotUseCase
import com.jiucaihua.app.domain.model.KLinePeriod
import javax.inject.Inject

class GetKLineDataTool @Inject constructor(
    private val buildKLineToolSnapshotUseCase: BuildKLineToolSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_kline_data",
        description = "获取指定标的的 K 线快照，包含周期、最新点位、最高最低价和完整点位序列。",
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
                    "description" to "返回的点位数量，默认 120",
                ),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = arguments["code"] as? String ?: error("Missing required argument: code")
        val period = parsePeriod(arguments["period"] as? String)
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 120
        return ToolResult(buildKLineToolSnapshotUseCase(code.trim(), period, limit))
    }

    private fun parsePeriod(value: String?): KLinePeriod {
        return value?.trim()?.uppercase()?.let(KLinePeriod::valueOf) ?: KLinePeriod.DAILY
    }
}
