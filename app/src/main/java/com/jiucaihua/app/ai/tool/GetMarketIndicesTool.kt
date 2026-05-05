package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildMarketIndicesSnapshotUseCase
import javax.inject.Inject

class GetMarketIndicesTool @Inject constructor(
    private val buildMarketIndicesSnapshotUseCase: BuildMarketIndicesSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_market_indices",
        description = "获取各市场主要指数行情，包括A股、港股、美股和黄金指数。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "market" to mapOf(
                    "type" to "string",
                    "enum" to listOf("A_STOCK", "HK_STOCK", "US_STOCK", "GOLD"),
                    "description" to "市场类型筛选，不传则返回全部市场",
                ),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val market = (arguments["market"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        return ToolResult(buildMarketIndicesSnapshotUseCase(market))
    }
}
