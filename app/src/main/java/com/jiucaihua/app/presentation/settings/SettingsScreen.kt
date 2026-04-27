package com.jiucaihua.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.ai.model.AiProvider
import com.jiucaihua.app.ai.model.metadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("行情刷新")
            RefreshIntervalSelector(
                currentInterval = uiState.refreshIntervalSeconds,
                onSelect = { viewModel.setRefreshInterval(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("外观")
            ThemeSelector(
                isDarkMode = uiState.isDarkMode,
                onSelect = { viewModel.setDarkMode(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("通知")
            SettingSwitch(
                title = "价格预警通知",
                subtitle = "当证券价格达到预警阈值时推送通知",
                checked = uiState.alertsEnabled,
                onCheckedChange = { viewModel.setAlertsEnabled(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("AI 助手")
            AiProviderSelector(
                selectedProvider = uiState.aiConfig.provider,
                onSelect = { viewModel.setAiProvider(it) },
            )
            SettingTextField(
                title = "API Key",
                value = uiState.aiConfig.apiKey,
                onValueChange = viewModel::setAiApiKey,
                placeholder = "输入 API Key",
                isSecret = true,
            )
            SettingTextField(
                title = "Base URL",
                value = if (uiState.aiConfig.provider == AiProvider.CUSTOM) uiState.aiConfig.baseUrl else uiState.aiConfig.effectiveBaseUrl,
                onValueChange = viewModel::setAiBaseUrl,
                placeholder = uiState.aiConfig.provider.metadata.defaultBaseUrl,
                enabled = uiState.aiConfig.provider == AiProvider.CUSTOM,
            )
            SettingTextField(
                title = "模型",
                value = uiState.aiConfig.preferredModel,
                onValueChange = viewModel::setAiModel,
                placeholder = uiState.aiConfig.provider.metadata.recommendedModel,
            )
            if (uiState.availableModels.isNotEmpty()) {
                ModelSelector(
                    models = uiState.availableModels,
                    selectedModel = uiState.aiConfig.preferredModel,
                    onSelect = viewModel::selectDiscoveredModel,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = viewModel::discoverAvailableModels,
                    enabled = !uiState.isLoadingModels && uiState.aiConfig.apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (uiState.isLoadingModels) "获取中..." else "获取可用模型")
                }
                Button(
                    onClick = viewModel::testAiConnection,
                    enabled = !uiState.isTestingConnection && uiState.aiConfig.apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (uiState.isTestingConnection) "测试中..." else "测试连通性")
                }
            }
            uiState.connectivityMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            SettingSwitch(
                title = "启用深度思考",
                subtitle = "允许 provider 使用更高推理强度",
                checked = uiState.aiConfig.enableThinking,
                onCheckedChange = { viewModel.setAiThinkingEnabled(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("关于")
            SettingItem(title = "版本", subtitle = "1.0.0")
            SettingItem(title = "应用名称", subtitle = "九财花")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RefreshIntervalSelector(
    currentInterval: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(5 to "5秒", 10 to "10秒", 15 to "15秒", 30 to "30秒")
    options.forEach { (seconds, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(seconds) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = currentInterval == seconds,
                onClick = { onSelect(seconds) },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    isDarkMode: Boolean?,
    onSelect: (Boolean?) -> Unit,
) {
    val options = listOf(
        null to "跟随系统",
        false to "浅色模式",
        true to "深色模式",
    )
    options.forEach { (mode, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(mode) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isDarkMode == mode,
                onClick = { onSelect(mode) },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun AiProviderSelector(
    selectedProvider: AiProvider,
    onSelect: (AiProvider) -> Unit,
) {
    AiProvider.entries.forEach { provider ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(provider) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedProvider == provider,
                onClick = { onSelect(provider) },
            )
            Text(
                text = provider.metadata.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<String>,
    selectedModel: String,
    onSelect: (String) -> Unit,
) {
    models.forEach { model ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(model) }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedModel == model,
                onClick = { onSelect(model) },
            )
            Text(
                text = model,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(title) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
