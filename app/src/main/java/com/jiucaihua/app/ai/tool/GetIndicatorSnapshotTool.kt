package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildIndicatorSnapshotUseCase
import javax.inject.Inject

class GetIndicatorSnapshotTool @Inject constructor(
    private val buildIndicatorSnapshotUseCase: BuildIndicatorSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_indicator_snapshot",
        description = "获取指定标的的技术指标快照，用于量化规则判定。返回判定所需的汇总指标而非完整K线序列。" +
            "支持股票和基金。如需查看完整K线数据，请使用 get_kline_data 工具。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "证券或基金代码，例如 sh600519、hk00700、110011",
                ),
                "cost_price" to mapOf(
                    "type" to "number",
                    "description" to "持仓成本价（可选）。提供后将计算浮盈、峰值浮盈等持仓相关指标",
                ),
                "hold_days" to mapOf(
                    "type" to "integer",
                    "description" to "持仓交易日数（可选）。提供后将参与中轨道晋升和脱离规则判定",
                ),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = arguments["code"] as? String ?: error("Missing required argument: code")
        val costPrice = (arguments["cost_price"] as? Number)?.toDouble()
        val holdDays = (arguments["hold_days"] as? Number)?.toInt()
        return ToolResult(buildIndicatorSnapshotUseCase(code.trim(), costPrice, holdDays))
    }
}
