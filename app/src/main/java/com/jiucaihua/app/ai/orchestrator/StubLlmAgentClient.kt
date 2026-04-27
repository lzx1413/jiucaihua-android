package com.jiucaihua.app.ai.orchestrator

import com.jiucaihua.app.ai.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubLlmAgentClient @Inject constructor() : LlmAgentClient {
    override suspend fun nextStep(
        systemPrompt: String,
        conversation: List<ChatMessage>,
        toolDefinitions: List<Map<String, Any?>>,
    ): AgentStep {
        return AgentStep(
            message = "LLM provider is not configured yet.",
            toolCall = null,
        )
    }
}
