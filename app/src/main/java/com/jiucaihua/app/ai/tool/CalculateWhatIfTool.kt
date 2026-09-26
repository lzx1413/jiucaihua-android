package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildWhatIfAnalysisSnapshotUseCase
import javax.inject.Inject

class CalculateWhatIfTool @Inject constructor(
    private val buildWhatIfAnalysisSnapshotUseCase: BuildWhatIfAnalysisSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "calculate_what_if",
        description = "对指定持仓执行目标价或涨跌幅假设推演，返回目标市值与盈亏变化。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "持仓代码，例如 sh600519、hk00700、110011",
                ),
                "targetPrice" to mapOf(
                    "type" to "number",
                    "exclusiveMinimum" to 0,
                    "description" to "目标价格，与 changePercent 二选一。",
                ),
                "changePercent" to mapOf(
                    "type" to "number",
                    "exclusiveMinimum" to -100,
                    "description" to "假设涨跌幅百分比，与 targetPrice 二选一。",
                ),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = (arguments["code"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: invalidArgs("code is required")
        val targetPrice = (arguments["targetPrice"] as? Number)?.toDouble()
        val changePercent = (arguments["changePercent"] as? Number)?.toDouble()
        if ((targetPrice != null) == (changePercent != null)) {
            invalidArgs("provide exactly one of targetPrice or changePercent")
        }
        return ToolResult(buildWhatIfAnalysisSnapshotUseCase(code.trim(), targetPrice, changePercent))
    }
}
