package com.jiucaihua.app.presentation.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.ai.config.AiConfigStore
import com.jiucaihua.app.ai.model.AiConfig
import com.jiucaihua.app.ai.model.AiProvider
import com.jiucaihua.app.ai.model.metadata
import com.jiucaihua.app.ai.provider.AiProviderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class SettingsUiState(
    val refreshIntervalSeconds: Int = 10,
    val isDarkMode: Boolean? = null,
    val oledMode: Boolean = false,
    val alertsEnabled: Boolean = true,
    val aiConfig: AiConfig = AiConfig(),
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectivityMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @Named("appPrefs") private val prefs: SharedPreferences,
    private val aiConfigStore: AiConfigStore,
    private val aiProviderService: AiProviderService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        val storedAiConfig = aiConfigStore.get()
        val aiConfig = storedAiConfig.withProviderDefaults()
        if (aiConfig != storedAiConfig) {
            aiConfigStore.save(aiConfig)
        }
        return SettingsUiState(
            refreshIntervalSeconds = prefs.getInt(KEY_REFRESH_INTERVAL, 10),
            isDarkMode = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null,
            oledMode = prefs.getBoolean(KEY_OLED_MODE, false),
            alertsEnabled = prefs.getBoolean(KEY_ALERTS_ENABLED, true),
            aiConfig = aiConfig,
        )
    }

    fun setRefreshInterval(seconds: Int) {
        prefs.edit().putInt(KEY_REFRESH_INTERVAL, seconds).apply()
        _uiState.value = _uiState.value.copy(refreshIntervalSeconds = seconds)
    }

    fun setDarkMode(enabled: Boolean?) {
        if (enabled == null) {
            prefs.edit().remove(KEY_DARK_MODE).apply()
        } else {
            prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        }
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun setOledMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OLED_MODE, enabled).apply()
        _uiState.value = _uiState.value.copy(oledMode = enabled)
    }

    fun setAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        _uiState.value = _uiState.value.copy(alertsEnabled = enabled)
    }

    fun setAiProvider(provider: AiProvider) {
        updateAiConfig {
            copy(
                provider = provider,
                baseUrl = if (provider == AiProvider.CUSTOM) baseUrl else "",
                preferredModel = if (provider == AiProvider.CUSTOM) preferredModel else provider.metadata.recommendedModel,
            )
        }
        _uiState.value = _uiState.value.copy(
            availableModels = emptyList(),
            connectivityMessage = null,
        )
    }

    fun setAiApiKey(apiKey: String) {
        updateAiConfig { copy(apiKey = apiKey) }
    }

    fun setAiBaseUrl(baseUrl: String) {
        updateAiConfig { copy(baseUrl = baseUrl) }
    }

    fun setAiModel(model: String) {
        updateAiConfig { copy(preferredModel = model) }
    }

    fun setAiThinkingEnabled(enabled: Boolean) {
        updateAiConfig { copy(enableThinking = enabled) }
    }

    fun discoverAvailableModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingModels = true,
                connectivityMessage = null,
            )
            runCatching {
                aiProviderService.discoverModels(_uiState.value.aiConfig)
            }.onSuccess { result ->
                val selectedModel = result.migratedModel
                    ?: _uiState.value.aiConfig.preferredModel.ifBlank { result.selectedModelSuggestion.orEmpty() }
                if (selectedModel.isNotBlank() && selectedModel != _uiState.value.aiConfig.preferredModel) {
                    updateAiConfig { copy(preferredModel = selectedModel) }
                }
                _uiState.value = _uiState.value.copy(
                    availableModels = result.models,
                    isLoadingModels = false,
                    connectivityMessage = if (result.models.isEmpty()) "未发现可用模型" else "已发现 ${result.models.size} 个模型",
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoadingModels = false,
                    connectivityMessage = it.message ?: "获取模型失败",
                )
            }
        }
    }

    fun selectDiscoveredModel(model: String) {
        updateAiConfig { copy(preferredModel = model) }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingConnection = true,
                connectivityMessage = null,
            )
            val result = aiProviderService.testConnection(_uiState.value.aiConfig)
            _uiState.value = _uiState.value.copy(
                isTestingConnection = false,
                connectivityMessage = result.message,
            )
        }
    }

    private fun updateAiConfig(transform: AiConfig.() -> AiConfig) {
        val updated = _uiState.value.aiConfig.transform().withProviderDefaults()
        aiConfigStore.save(updated)
        _uiState.value = _uiState.value.copy(aiConfig = updated)
    }

    private fun AiConfig.withProviderDefaults(): AiConfig {
        val normalizedModel = when {
            preferredModel.isBlank() -> provider.metadata.recommendedModel
            else -> preferredModel
        }
        return copy(preferredModel = normalizedModel)
    }

    companion object {
        const val KEY_REFRESH_INTERVAL = "refresh_interval_seconds"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_OLED_MODE = "oled_mode"
        const val KEY_ALERTS_ENABLED = "alerts_enabled"
    }
}
