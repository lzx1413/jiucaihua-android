package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildMarketStatusSnapshotUseCase
import javax.inject.Inject

class GetMarketStatusTool @Inject constructor(
    private val buildMarketStatusSnapshotUseCase: BuildMarketStatusSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_market_status",
        description = "获取各市场交易状态、是否为节假日以及港币兑人民币汇率。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>(),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildMarketStatusSnapshotUseCase())
    }
}
