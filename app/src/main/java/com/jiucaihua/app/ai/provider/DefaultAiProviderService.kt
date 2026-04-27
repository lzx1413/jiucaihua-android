package com.jiucaihua.app.ai.provider

import com.jiucaihua.app.ai.model.AiConfig
import com.jiucaihua.app.ai.model.AiProvider
import com.jiucaihua.app.ai.model.ConnectivityTestResult
import com.jiucaihua.app.ai.model.ProviderModelsResult
import com.jiucaihua.app.ai.model.metadata
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Singleton
class DefaultAiProviderService @Inject constructor() : AiProviderService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override suspend fun discoverModels(config: AiConfig): ProviderModelsResult {
        return when (config.provider) {
            AiProvider.DEEPSEEK -> discoverDeepSeekModels(config)
            else -> ProviderModelsResult(
                models = listOfNotNull(config.preferredModel.takeIf { it.isNotBlank() }, config.provider.metadata.recommendedModel).distinct(),
                selectedModelSuggestion = config.preferredModel.ifBlank { config.provider.metadata.recommendedModel }.ifBlank { null },
            )
        }
    }

    override suspend fun testConnection(config: AiConfig): ConnectivityTestResult {
        return runCatching {
            val result = discoverModels(config)
            if (result.models.isNotEmpty()) {
                ConnectivityTestResult(true, "连接成功，已获取 ${result.models.size} 个模型")
            } else {
                ConnectivityTestResult(false, "未获取到可用模型")
            }
        }.getOrElse {
            ConnectivityTestResult(false, it.message ?: "连接失败")
        }
    }

    private suspend fun discoverDeepSeekModels(config: AiConfig): ProviderModelsResult {
        val apiKey = config.apiKey.trim()
        require(apiKey.isNotEmpty()) { "请先填写 API Key" }
        val api = Retrofit.Builder()
            .baseUrl(config.effectiveBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeepSeekApi::class.java)
        val models = api.getModels("Bearer $apiKey").data
            .map { it.id }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val migratedModel = normalizePreferredModel(config.preferredModel, models)
        return ProviderModelsResult(
            models = models,
            selectedModelSuggestion = migratedModel
                ?: models.firstOrNull { it == config.provider.metadata.recommendedModel }
                ?: models.firstOrNull()
                ?: config.provider.metadata.recommendedModel,
            migratedModel = migratedModel,
        )
    }

    private fun normalizePreferredModel(
        preferredModel: String,
        discoveredModels: List<String>,
    ): String? {
        val trimmed = preferredModel.trim()
        if (trimmed.isBlank()) return null
        if (trimmed in discoveredModels) return trimmed
        val aliases = when (trimmed) {
            "deepseek-chat", "deepseek-v3", "deepseek-v4", "deepseek-v4-flash" -> listOf(
                "deepseek-chat",
                "deepseek-v3.1",
                "deepseek-v3.1-base",
                "deepseek-v3-0324",
                "deepseek-v4",
                "deepseek-v4-base",
                "deepseek-v4-flash",
            )
            "deepseek-reasoner" -> listOf(
                "deepseek-reasoner",
                "deepseek-r1",
                "deepseek-r1-0528",
            )
            else -> listOf(trimmed)
        }
        return aliases.firstOrNull { it in discoveredModels }
    }
}
