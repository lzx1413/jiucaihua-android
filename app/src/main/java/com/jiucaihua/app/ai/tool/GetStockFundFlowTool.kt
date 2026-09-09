package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.repository.SecurityEventRepository
import com.jiucaihua.app.domain.model.UnsupportedSecurityMarketException
import javax.inject.Inject

class GetStockFundFlowTool @Inject constructor(
    private val securityEventRepository: SecurityEventRepository,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_stock_fund_flow",
        description = "获取单只 A 股的腾讯个股资金流，不等同于沪深港通资金流。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string"),
                "period" to mapOf("type" to "string", "enum" to listOf("intraday", "5d")),
            ),
            "required" to listOf("code"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = GetStockEventsTool.parseSecurityId(arguments["code"])
        val snapshot = try {
            securityEventRepository.getStockFundFlow(code)
        } catch (error: UnsupportedSecurityMarketException) {
            unsupportedMarket(error.message ?: "unsupported market")
        }
        return snapshot?.let { ToolResult(it.toToolSnapshot()) }
            ?: ToolResult(error = ToolError("PROVIDER_UNAVAILABLE", "No stock fund-flow data is available"))
    }
}
