package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildPortfolioPerformanceToolSnapshotUseCase
import javax.inject.Inject

class GetPortfolioPerformanceTool @Inject constructor(
    private val buildPortfolioPerformanceToolSnapshotUseCase: BuildPortfolioPerformanceToolSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_portfolio_performance",
        description = "获取组合真实收益概览，基于总资产、现金、已实现收益、浮动收益、分红、费用税费和外部入金出金计算。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "from" to mapOf("type" to "number", "description" to "可选，开始时间戳毫秒"),
                "to" to mapOf("type" to "number", "description" to "可选，结束时间戳毫秒"),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val from = (arguments["from"] as? Number)?.toLong()
        val to = (arguments["to"] as? Number)?.toLong()
        return ToolResult(buildPortfolioPerformanceToolSnapshotUseCase(from, to))
    }
}
