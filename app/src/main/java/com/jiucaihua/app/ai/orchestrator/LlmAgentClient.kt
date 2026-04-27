package com.jiucaihua.app.ai.orchestrator

import com.jiucaihua.app.ai.model.ChatMessage
import com.jiucaihua.app.ai.model.ToolCall

interface LlmAgentClient {
    suspend fun nextStep(
        systemPrompt: String,
        conversation: List<ChatMessage>,
        toolDefinitions: List<Map<String, Any?>>,
    ): AgentStep
}

data class AgentStep(
    val message: String,
    val toolCall: ToolCall? = null,
    val reasoningContent: String? = null,
)
