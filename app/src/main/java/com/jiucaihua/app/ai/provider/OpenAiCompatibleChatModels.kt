package com.jiucaihua.app.ai.provider

import com.squareup.moshi.Json

data class OpenAiCompatibleChatRequest(
    val model: String,
    val messages: List<OpenAiCompatibleMessage>,
    val tools: List<OpenAiCompatibleTool> = emptyList(),
)

data class OpenAiCompatibleMessage(
    val role: String,
    val content: String? = null,
    @Json(name = "tool_calls")
    val toolCalls: List<OpenAiCompatibleToolCall>? = null,
    @Json(name = "tool_call_id")
    val toolCallId: String? = null,
    val name: String? = null,
    @Json(name = "reasoning_content")
    val reasoningContent: String? = null,
)

data class OpenAiCompatibleTool(
    val type: String = "function",
    val function: OpenAiCompatibleFunctionDefinition,
)

data class OpenAiCompatibleFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>,
)

data class OpenAiCompatibleChatResponse(
    val choices: List<OpenAiCompatibleChoice> = emptyList(),
)

data class OpenAiCompatibleChoice(
    val message: OpenAiCompatibleAssistantMessage,
)

data class OpenAiCompatibleAssistantMessage(
    val content: String? = null,
    @Json(name = "tool_calls")
    val toolCalls: List<OpenAiCompatibleToolCall>? = null,
    @Json(name = "reasoning_content")
    val reasoningContent: String? = null,
)

data class OpenAiCompatibleToolCall(
    val id: String,
    val type: String,
    val function: OpenAiCompatibleFunctionCall,
)

data class OpenAiCompatibleFunctionCall(
    val name: String,
    val arguments: String,
)
