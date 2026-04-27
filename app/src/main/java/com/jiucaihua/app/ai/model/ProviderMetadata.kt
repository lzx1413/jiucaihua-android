package com.jiucaihua.app.ai.model

data class ProviderMetadata(
    val displayName: String,
    val defaultBaseUrl: String,
    val recommendedModel: String,
)

val AiProvider.metadata: ProviderMetadata
    get() = when (this) {
        AiProvider.CLAUDE -> ProviderMetadata(
            displayName = "Claude",
            defaultBaseUrl = "https://api.anthropic.com/",
            recommendedModel = "claude-opus-4-7",
        )
        AiProvider.OPENAI -> ProviderMetadata(
            displayName = "OpenAI",
            defaultBaseUrl = "https://api.openai.com/",
            recommendedModel = "gpt-4o-mini",
        )
        AiProvider.DEEPSEEK -> ProviderMetadata(
            displayName = "DeepSeek",
            defaultBaseUrl = "https://api.deepseek.com/",
            recommendedModel = "deepseek-chat",
        )
        AiProvider.CUSTOM -> ProviderMetadata(
            displayName = "Custom",
            defaultBaseUrl = "",
            recommendedModel = "",
        )
    }
