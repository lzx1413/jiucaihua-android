package com.jiucaihua.app.ai.tool

interface ToolExecutor {
    val definition: ToolDefinition

    val name: String
        get() = definition.name

    val description: String
        get() = definition.description

    val inputSchema: Map<String, Any?>
        get() = definition.inputSchema

    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}

data class ToolResult(
    val content: Any? = null,
    val error: ToolError? = null,
)

data class ToolError(
    val code: String,
    val message: String,
)

class ToolExecutionException(val toolError: ToolError) : IllegalArgumentException(toolError.message)

fun invalidArgs(message: String): Nothing = throw ToolExecutionException(ToolError("INVALID_ARGS", message))

fun unsupportedMarket(message: String): Nothing = throw ToolExecutionException(ToolError("UNSUPPORTED_MARKET", message))

internal fun normalizedQuoteCode(value: Any?): String {
    val raw = (value as? String)?.trim() ?: invalidArgs("code is required")
    return com.jiucaihua.app.domain.model.SecurityId.parse(raw)?.value
        ?: raw.takeIf { it in com.jiucaihua.app.domain.model.MarketIndexCodes.GOLD_INDICES || it in com.jiucaihua.app.domain.model.MarketIndexCodes.HK_STOCK_INDICES }
        ?: invalidArgs("code must be normalized, e.g. sh600519, hk00700, usr_AAPL, or 110011")
}
