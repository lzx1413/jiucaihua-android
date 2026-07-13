package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildTransactionsToolSnapshotUseCase
import com.jiucaihua.app.ai.usecase.parseTransactionQuery
import javax.inject.Inject

class GetTransactionsTool @Inject constructor(
    private val buildTransactionsToolSnapshotUseCase: BuildTransactionsToolSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_transactions",
        description = "查询投资交易流水明细，可按证券代码、市场、交易类型和时间范围过滤。用于分析持仓成本、买卖节奏、分红、费用、税费和现金流。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string", "description" to "可选，证券代码"),
                "market_type" to mapOf("type" to "string", "description" to "可选，A_STOCK/HK_STOCK/US_STOCK/FUND/GOLD"),
                "type" to mapOf("type" to "string", "description" to "可选，BUY/SELL/DIVIDEND/FEE/TAX/SPLIT/CASH_IN/CASH_OUT"),
                "from" to mapOf("type" to "number", "description" to "可选，开始时间戳毫秒"),
                "to" to mapOf("type" to "number", "description" to "可选，结束时间戳毫秒"),
                "limit" to mapOf("type" to "number", "description" to "返回条数，默认 50，最大 200"),
                "offset" to mapOf("type" to "number", "description" to "分页偏移，默认 0"),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildTransactionsToolSnapshotUseCase(parseTransactionQuery(arguments)))
    }
}
