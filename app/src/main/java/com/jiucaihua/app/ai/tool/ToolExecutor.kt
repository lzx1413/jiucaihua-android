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
