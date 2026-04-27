package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildMarketNewsDigestUseCase
import javax.inject.Inject

class GetMarketNewsTool @Inject constructor(
    private val buildMarketNewsDigestUseCase: BuildMarketNewsDigestUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_market_news",
        description = "获取市场资讯摘要，返回最新资讯列表和生成时间。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "limit" to mapOf(
                    "type" to "integer",
                    "description" to "返回的资讯条数，默认 10",
                ),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 10
        return ToolResult(buildMarketNewsDigestUseCase(limit))
    }
}
