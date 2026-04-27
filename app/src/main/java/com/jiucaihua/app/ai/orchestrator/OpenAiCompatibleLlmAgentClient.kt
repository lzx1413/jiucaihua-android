package com.jiucaihua.app.ai.orchestrator

import android.util.Log
import com.jiucaihua.app.ai.config.AiConfigStore
import com.jiucaihua.app.ai.model.AiProvider
import com.jiucaihua.app.ai.model.ChatMessage
import com.jiucaihua.app.ai.model.ChatRole
import com.jiucaihua.app.ai.model.ToolCall
import com.jiucaihua.app.ai.provider.OpenAiCompatibleAssistantMessage
import com.jiucaihua.app.ai.provider.OpenAiCompatibleChatApi
import com.jiucaihua.app.ai.provider.OpenAiCompatibleChatRequest
import com.jiucaihua.app.ai.provider.OpenAiCompatibleFunctionCall
import com.jiucaihua.app.ai.provider.OpenAiCompatibleFunctionDefinition
import com.jiucaihua.app.ai.provider.OpenAiCompatibleMessage
import com.jiucaihua.app.ai.provider.OpenAiCompatibleTool
import com.jiucaihua.app.ai.provider.OpenAiCompatibleToolCall
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Singleton
class OpenAiCompatibleLlmAgentClient @Inject constructor(
    private val aiConfigStore: AiConfigStore,
) : LlmAgentClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val toolArgumentsAdapter = moshi.adapter<Map<String, Any?>>(mapType)

    override suspend fun nextStep(
        systemPrompt: String,
        conversation: List<ChatMessage>,
        toolDefinitions: List<Map<String, Any?>>,
    ): AgentStep {
        val config = aiConfigStore.get()
        require(config.provider == AiProvider.DEEPSEEK || config.provider == AiProvider.OPENAI || config.provider == AiProvider.CUSTOM) {
            "当前 provider 暂不支持真实对话：${config.provider}"
        }
        val apiKey = config.apiKey.trim()
        require(apiKey.isNotEmpty()) { "请先在设置中填写 API Key" }
        val model = config.preferredModel.trim()
        require(model.isNotEmpty()) { "请先在设置中选择模型" }

        val api = Retrofit.Builder()
            .baseUrl(config.effectiveBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiCompatibleChatApi::class.java)

        val request = OpenAiCompatibleChatRequest(
            model = model,
            messages = buildMessages(systemPrompt, conversation),
            tools = buildTools(toolDefinitions),
        )
        val response = api.createChatCompletion(
            authorization = "Bearer $apiKey",
            request = request,
        )
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string().orEmpty()
            Log.e(LOG_TAG, "Chat completion failed code=${response.code()} body=$errorBody request=${request.toDebugString()}")
            error(
                buildString {
                    append("LLM 请求失败(${response.code()})")
                    val detail = errorBody.ifBlank { response.message() }
                    if (detail.isNotBlank()) {
                        append(": ")
                        append(detail)
                    }
                },
            )
        }
        val body = response.body() ?: error("Provider returned empty body")
        val message = body.choices.firstOrNull()?.message ?: error("Provider returned no choices")
        return message.toAgentStep()
    }

    private fun buildMessages(
        systemPrompt: String,
        conversation: List<ChatMessage>,
    ): List<OpenAiCompatibleMessage> {
        return buildList {
            add(OpenAiCompatibleMessage(role = "system", content = systemPrompt))
            conversation.forEach { message ->
                val hasToolCalls = message.toolCalls.isNotEmpty()
                add(
                    OpenAiCompatibleMessage(
                        role = message.role.toProviderRole(),
                        content = when {
                            hasToolCalls && message.role == ChatRole.ASSISTANT && message.content.isBlank() -> ""
                            else -> message.content.takeIf { it.isNotBlank() }
                        },
                        toolCalls = message.toolCalls.takeIf { it.isNotEmpty() }?.map { toolCall ->
                            OpenAiCompatibleToolCall(
                                id = toolCall.id,
                                type = "function",
                                function = OpenAiCompatibleFunctionCall(
                                    name = toolCall.name,
                                    arguments = toolArgumentsAdapter.toJson(toolCall.arguments),
                                ),
                            )
                        },
                        toolCallId = message.toolCallId,
                        name = message.toolName,
                        reasoningContent = message.reasoningContent.takeIf { it?.isNotBlank() == true },
                    ),
                )
            }
        }
    }

    private fun buildTools(toolDefinitions: List<Map<String, Any?>>): List<OpenAiCompatibleTool> {
        return toolDefinitions.map { definition ->
            OpenAiCompatibleTool(
                function = OpenAiCompatibleFunctionDefinition(
                    name = definition["name"] as? String ?: error("Missing tool name"),
                    description = definition["description"] as? String ?: "",
                    parameters = (definition["inputSchema"] as? Map<*, *>)
                        ?.entries
                        ?.associate { (key, value) -> key as String to value }
                        ?: emptyMap(),
                ),
            )
        }
    }

    private fun OpenAiCompatibleAssistantMessage.toAgentStep(): AgentStep {
        val firstToolCall = toolCalls?.firstOrNull()
        return AgentStep(
            message = content.orEmpty(),
            toolCall = firstToolCall?.let {
                ToolCall(
                    id = it.id,
                    name = it.function.name,
                    arguments = parseArguments(it.function.arguments),
                )
            },
            reasoningContent = reasoningContent,
        )
    }

    private fun parseArguments(json: String): Map<String, Any?> {
        return toolArgumentsAdapter.fromJson(json).orEmpty()
    }

    private fun ChatRole.toProviderRole(): String {
        return when (this) {
            ChatRole.SYSTEM -> "system"
            ChatRole.USER -> "user"
            ChatRole.ASSISTANT -> "assistant"
            ChatRole.TOOL -> "tool"
        }
    }

    private fun OpenAiCompatibleChatRequest.toDebugString(): String {
        return buildString {
            append("model=")
            append(model)
            append(", messages=")
            append(messages.joinToString(prefix = "[", postfix = "]") { message ->
                buildString {
                    append("{role=")
                    append(message.role)
                    append(", content=")
                    append(message.content)
                    append(", toolCallId=")
                    append(message.toolCallId)
                    append(", name=")
                    append(message.name)
                    append(", reasoningContent=")
                    append(message.reasoningContent != null)
                    append(", toolCalls=")
                    append(message.toolCalls?.map { it.function.name to it.id })
                    append("}")
                }
            })
            append(", tools=")
            append(tools.map { it.function.name })
        }
    }

    companion object {
        private const val LOG_TAG = "Jiucaihua-AI"

        private val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java,
        )
    }
}
