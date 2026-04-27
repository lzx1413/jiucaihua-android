package com.jiucaihua.app.ai.model

data class ProviderModelsResult(
    val models: List<String>,
    val selectedModelSuggestion: String? = null,
    val migratedModel: String? = null,
)

data class ConnectivityTestResult(
    val isSuccess: Boolean,
    val message: String,
)
