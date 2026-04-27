package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildPortfolioAnalysisSnapshotUseCase
import javax.inject.Inject

class GetPortfolioAnalysisTool @Inject constructor(
    private val buildPortfolioAnalysisSnapshotUseCase: BuildPortfolioAnalysisSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_portfolio_analysis",
        description = "获取当前投资组合的全局分析快照，包含组合总览、持仓列表、市场状态、预警摘要和数据新鲜度。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>(),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildPortfolioAnalysisSnapshotUseCase())
    }
}
