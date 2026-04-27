package com.jiucaihua.app.ai.model

enum class AiProvider {
    CLAUDE,
    OPENAI,
    DEEPSEEK,
    CUSTOM,
}

data class AiConfig(
    val provider: AiProvider = AiProvider.CLAUDE,
    val apiKey: String = "",
    val baseUrl: String = "",
    val preferredModel: String = "",
    val enableThinking: Boolean = true,
) {
    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { provider.metadata.defaultBaseUrl }
}
