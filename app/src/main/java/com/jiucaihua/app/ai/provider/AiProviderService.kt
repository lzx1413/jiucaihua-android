package com.jiucaihua.app.ai.provider

import com.jiucaihua.app.ai.model.AiConfig
import com.jiucaihua.app.ai.model.ConnectivityTestResult
import com.jiucaihua.app.ai.model.ProviderModelsResult

interface AiProviderService {
    suspend fun discoverModels(config: AiConfig): ProviderModelsResult
    suspend fun testConnection(config: AiConfig): ConnectivityTestResult
}
