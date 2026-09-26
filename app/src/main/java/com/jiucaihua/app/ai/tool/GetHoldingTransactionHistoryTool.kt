package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildHoldingTransactionHistorySnapshotUseCase
import com.jiucaihua.app.domain.model.MarketType
import javax.inject.Inject

class GetHoldingTransactionHistoryTool @Inject constructor(
    private val buildHoldingTransactionHistorySnapshotUseCase: BuildHoldingTransactionHistorySnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_holding_transaction_history",
        description = "获取单个标的的交易历史和收益拆解，包含当前持仓、平均成本、已实现收益、浮动收益和交易列表。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string", "description" to "证券代码，例如 sh600519、hk00700、110011"),
                "market_type" to mapOf("type" to "string", "description" to "可选，A_STOCK/HK_STOCK/US_STOCK/FUND/GOLD"),
                "offset" to mapOf("type" to "integer", "minimum" to 0, "maximum" to Int.MAX_VALUE, "description" to "分页偏移，默认0；汇总不受分页影响"),
                "limit" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 200, "description" to "返回交易条数，默认 100，最大 200"),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = (arguments["code"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: invalidArgs("code is required")
        val marketType = (arguments["market_type"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            runCatching { MarketType.valueOf(it.uppercase()) }.getOrElse { invalidArgs("market_type is invalid") }
        }
        val limit = (arguments["limit"] as? Number)?.toInt()?.coerceIn(1, 200) ?: 100
        return ToolResult(buildHoldingTransactionHistorySnapshotUseCase(code, marketType, limit, (arguments["offset"] as? Number)?.toInt() ?: 0))
    }
}
