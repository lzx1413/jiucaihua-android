package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildFundFlowSnapshotUseCase
import javax.inject.Inject

class GetFundFlowTool @Inject constructor(
    private val buildFundFlowSnapshotUseCase: BuildFundFlowSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_fund_flow",
        description = "获取沪深港通资金流向数据，包括北向资金（沪股通、深股通）和南向资金（港股通）的净流入、净买入和余额。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>(),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildFundFlowSnapshotUseCase())
    }
}
