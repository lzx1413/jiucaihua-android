package com.jiucaihua.app.ai.orchestrator

import com.jiucaihua.app.ai.model.AgentIteration
import com.jiucaihua.app.ai.model.AgentRunResult
import com.jiucaihua.app.ai.model.ChatMessage
import com.jiucaihua.app.ai.model.ChatRole
import com.jiucaihua.app.ai.tool.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAgentOrchestrator @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val llmAgentClient: LlmAgentClient,
) {
    suspend fun run(
        conversation: List<ChatMessage>,
        userMessage: String,
        maxIterations: Int = 4,
    ): AgentRunResult {
        var currentConversation = conversation + ChatMessage(
            role = ChatRole.USER,
            content = userMessage,
        )
        val iterations = mutableListOf<AgentIteration>()
        val toolDefinitions = toolRegistry.getDefinitions().map {
            mapOf(
                "name" to it.name,
                "description" to it.description,
                "inputSchema" to it.inputSchema,
            )
        }

        repeat(maxIterations) {
            val step = llmAgentClient.nextStep(
                systemPrompt = systemPromptBuilder.build(),
                conversation = currentConversation,
                toolDefinitions = toolDefinitions,
            )
            val toolCall = step.toolCall
            if (toolCall == null) {
                iterations += AgentIteration(assistantMessage = step.message)
                currentConversation = systemPromptBuilder.appendAssistantMessage(
                    conversation = currentConversation,
                    message = step.message,
                    reasoningContent = step.reasoningContent,
                )
                return AgentRunResult(
                    iterations = iterations,
                    finalMessage = step.message,
                    conversation = currentConversation,
                )
            }

            val toolResult = toolRegistry.execute(toolCall.name, toolCall.arguments).content
            iterations += AgentIteration(
                assistantMessage = step.message,
                toolCall = toolCall,
                toolResult = toolResult,
            )
            currentConversation = systemPromptBuilder.appendAssistantMessage(
                conversation = currentConversation,
                message = step.message,
                toolCalls = listOf(toolCall),
                reasoningContent = step.reasoningContent,
            )
            currentConversation = systemPromptBuilder.appendToolResult(currentConversation, toolCall, toolResult)
        }

        val finalMessage = "已达到本轮工具调用上限，请基于当前结果继续提问。"
        currentConversation = systemPromptBuilder.appendAssistantMessage(
            conversation = currentConversation,
            message = finalMessage,
        )
        return AgentRunResult(
            iterations = iterations,
            finalMessage = finalMessage,
            conversation = currentConversation,
        )
    }

    fun getAvailableToolNames(): List<String> {
        return toolRegistry.getDefinitions().map { it.name }
    }
}
