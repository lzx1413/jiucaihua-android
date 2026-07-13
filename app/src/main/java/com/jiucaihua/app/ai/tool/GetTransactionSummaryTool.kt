package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildTransactionSummaryToolSnapshotUseCase
import com.jiucaihua.app.ai.usecase.parseTransactionQuery
import javax.inject.Inject

class GetTransactionSummaryTool @Inject constructor(
    private val buildTransactionSummaryToolSnapshotUseCase: BuildTransactionSummaryToolSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_transaction_summary",
        description = "获取交易聚合摘要，包含买入、卖出、已实现收益、分红、手续费、税费、入金和出金。用于分析收益来源和真实资金变化。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string", "description" to "可选，证券代码"),
                "market_type" to mapOf("type" to "string", "description" to "可选，A_STOCK/HK_STOCK/US_STOCK/FUND/GOLD"),
                "from" to mapOf("type" to "number", "description" to "可选，开始时间戳毫秒"),
                "to" to mapOf("type" to "number", "description" to "可选，结束时间戳毫秒"),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildTransactionSummaryToolSnapshotUseCase(parseTransactionQuery(arguments)))
    }
}
