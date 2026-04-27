package com.jiucaihua.app.ai.provider

data class DeepSeekModelsResponse(
    val data: List<DeepSeekModelItem> = emptyList(),
)

data class DeepSeekModelItem(
    val id: String,
)
