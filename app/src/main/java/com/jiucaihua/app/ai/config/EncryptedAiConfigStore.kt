package com.jiucaihua.app.ai.config

import android.content.SharedPreferences
import androidx.core.content.edit
import com.jiucaihua.app.ai.model.AiConfig
import com.jiucaihua.app.ai.model.AiProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EncryptedAiConfigStore @Inject constructor(
    @Named("aiPrefs") private val prefs: SharedPreferences,
) : AiConfigStore {
    override fun get(): AiConfig {
        return AiConfig(
            provider = prefs.getString(KEY_PROVIDER, AiProvider.CLAUDE.name)
                ?.let(AiProvider::valueOf)
                ?: AiProvider.CLAUDE,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            preferredModel = prefs.getString(KEY_MODEL, "") ?: "",
            enableThinking = prefs.getBoolean(KEY_ENABLE_THINKING, true),
        )
    }

    override fun save(config: AiConfig) {
        prefs.edit {
            putString(KEY_PROVIDER, config.provider.name)
            putString(KEY_API_KEY, config.apiKey)
            putString(KEY_BASE_URL, config.baseUrl)
            putString(KEY_MODEL, config.preferredModel)
            putBoolean(KEY_ENABLE_THINKING, config.enableThinking)
        }
    }

    companion object {
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_API_KEY = "ai_api_key"
        const val KEY_BASE_URL = "ai_base_url"
        const val KEY_MODEL = "ai_model"
        const val KEY_ENABLE_THINKING = "ai_enable_thinking"
    }
}
