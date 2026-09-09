package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.UnsupportedSecurityMarketException
import com.jiucaihua.app.domain.repository.SecurityEventRepository
import javax.inject.Inject

class GetStockEventsTool @Inject constructor(
    private val securityEventRepository: SecurityEventRepository,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_stock_events",
        description = "按九财花规范证券代码查询腾讯聚合的新闻、公告、定期报告和研报。每条结果包含来源、时间、新鲜度及原文链接。",
        inputSchema = schema,
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = parseSecurityId(arguments["code"])
        val kinds = parseKinds(arguments["kinds"])
        val limit = (arguments["limit"] as? Number)?.toInt()?.coerceIn(1, 50) ?: 10
        val events = try {
            securityEventRepository.getEvents(code, kinds, limit)
        } catch (error: UnsupportedSecurityMarketException) {
            unsupportedMarket(error.message ?: "unsupported market")
        }
        return ToolResult(StockEventsToolSnapshot(code.value, events.map { it.toToolSnapshot() }))
    }

    companion object {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf("type" to "string", "description" to "九财花规范代码，例如 sh600519、hk00700"),
                "kinds" to mapOf("type" to "array", "items" to mapOf("type" to "string", "enum" to SecurityEventKind.entries.map { it.name })),
                "limit" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 50),
            ),
            "required" to listOf("code"),
        )

        fun parseSecurityId(value: Any?): SecurityId = (value as? String)?.let(SecurityId::parse)
            ?: invalidArgs("code must be a supported normalized security code")

        fun parseKinds(value: Any?): Set<SecurityEventKind> {
            if (value == null) return setOf(SecurityEventKind.NEWS, SecurityEventKind.ANNOUNCEMENT, SecurityEventKind.PERIODIC_REPORT, SecurityEventKind.RESEARCH)
            val values = value as? List<*> ?: invalidArgs("kinds must be an array of event kinds")
            if (values.isEmpty()) invalidArgs("kinds must not be empty")
            return values.map { raw ->
                (raw as? String)?.trim()?.uppercase()?.let { runCatching { SecurityEventKind.valueOf(it) }.getOrNull() }
                    ?: invalidArgs("kinds contains an unsupported value")
            }.toSet()
        }
    }
}
