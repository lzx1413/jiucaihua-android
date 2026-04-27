package com.jiucaihua.app.ai.tool

data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any?>,
)
