package com.jiucaihua.app.ai.model

data class ChatMessage(
    val role: ChatRole,
    val content: String = "",
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null,
    val reasoningContent: String? = null,
)

enum class ChatRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?> = emptyMap(),
)

data class AgentIteration(
    val assistantMessage: String,
    val toolCall: ToolCall? = null,
    val toolResult: Any? = null,
)

data class AgentRunResult(
    val iterations: List<AgentIteration>,
    val finalMessage: String,
    val conversation: List<ChatMessage>,
)
