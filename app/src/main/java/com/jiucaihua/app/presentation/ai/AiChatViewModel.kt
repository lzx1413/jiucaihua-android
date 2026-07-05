package com.jiucaihua.app.presentation.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.R
import com.jiucaihua.app.ai.model.AgentIteration
import com.jiucaihua.app.ai.model.AgentRunResult
import com.jiucaihua.app.ai.model.ChatMessage
import com.jiucaihua.app.ai.orchestrator.AiAgentOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatMessageItem(
    val role: AiChatRole,
    val content: String,
)

enum class AiChatRole {
    USER,
    ASSISTANT,
    TOOL,
}

data class AiChatUiState(
    val input: String = "",
    val messages: List<AiChatMessageItem> = emptyList(),
    val conversation: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiAgentOrchestrator: AiAgentOrchestrator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
    }

    fun sendMessage() {
        val userMessage = _uiState.value.input.trim()
        if (userMessage.isBlank() || _uiState.value.isSending) return

        _uiState.value = _uiState.value.copy(
            input = "",
            isSending = true,
            error = null,
            messages = _uiState.value.messages + AiChatMessageItem(
                role = AiChatRole.USER,
                content = userMessage,
            ),
        )

        viewModelScope.launch {
            runCatching {
                aiAgentOrchestrator.run(
                    conversation = _uiState.value.conversation,
                    userMessage = userMessage,
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    messages = _uiState.value.messages + result.toChatItems(),
                    conversation = result.conversation,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = it.message ?: context.getString(R.string.ai_send_failed),
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun List<AgentIteration>.toChatItems(): List<AiChatMessageItem> {
        return flatMap { iteration ->
            buildList {
                if (iteration.assistantMessage.isNotBlank()) {
                    add(
                        AiChatMessageItem(
                            role = AiChatRole.ASSISTANT,
                            content = iteration.assistantMessage,
                        ),
                    )
                }
                iteration.toolCall?.let { toolCall ->
                    add(
                        AiChatMessageItem(
                            role = AiChatRole.TOOL,
                            content = buildString {
                                append(toolCall.name)
                                if (iteration.toolResult != null) {
                                    append("\n")
                                    append(iteration.toolResult)
                                }
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun AgentRunResult.toChatItems(): List<AiChatMessageItem> {
        val items = iterations.toChatItems().toMutableList()
        if (finalMessage.isNotBlank() && items.lastOrNull()?.content != finalMessage) {
            items += AiChatMessageItem(
                role = AiChatRole.ASSISTANT,
                content = finalMessage,
            )
        }
        return items
    }
}
