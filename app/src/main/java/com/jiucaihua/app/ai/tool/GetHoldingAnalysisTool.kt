package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildHoldingAnalysisSnapshotUseCase
import javax.inject.Inject

class GetHoldingAnalysisTool @Inject constructor(
    private val buildHoldingAnalysisSnapshotUseCase: BuildHoldingAnalysisSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_holding_analysis",
        description = "获取指定标的的单条目分析快照，包含持仓摘要、预警、相关新闻和数据新鲜度。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "证券代码，例如 sh600519、hk00700、110011",
                ),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = arguments["code"] as? String ?: error("Missing required argument: code")
        return ToolResult(buildHoldingAnalysisSnapshotUseCase(code.trim()))
    }
}
