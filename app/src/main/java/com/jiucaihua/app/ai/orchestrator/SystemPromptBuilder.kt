package com.jiucaihua.app.ai.orchestrator

import com.jiucaihua.app.ai.model.ChatMessage
import com.jiucaihua.app.ai.model.ChatRole
import com.jiucaihua.app.ai.model.ToolCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemPromptBuilder @Inject constructor() {
    fun build(): String {
        return """
            你是九财花的投资助手。
            你只能基于工具返回的数据回答，不要编造持仓、收益、新闻或预警信息。
            当用户问题需要结构化数据时，优先调用最合适的单个工具；必要时再继续调用下一个工具。
            如果工具结果为空或查不到数据，直接说明限制。
            回答保持简洁、准确，并明确引用关键结论。
        """.trimIndent()
    }

    fun buildInitialConversation(userMessage: String): List<ChatMessage> {
        return listOf(
            ChatMessage(role = ChatRole.USER, content = userMessage),
        )
    }

    fun appendAssistantMessage(
        conversation: List<ChatMessage>,
        message: String,
        toolCalls: List<ToolCall> = emptyList(),
        reasoningContent: String? = null,
    ): List<ChatMessage> {
        return conversation + ChatMessage(
            role = ChatRole.ASSISTANT,
            content = message,
            toolCalls = toolCalls,
            reasoningContent = reasoningContent,
        )
    }

    fun appendToolResult(
        conversation: List<ChatMessage>,
        toolCall: ToolCall,
        result: Any?,
    ): List<ChatMessage> {
        return conversation + ChatMessage(
            role = ChatRole.TOOL,
            content = serializeToolResult(result),
            toolCallId = toolCall.id,
            toolName = toolCall.name,
        )
    }

    private fun serializeToolResult(result: Any?): String {
        return result?.toString() ?: "null"
    }
}
