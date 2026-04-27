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
    val content: Any?,
)
