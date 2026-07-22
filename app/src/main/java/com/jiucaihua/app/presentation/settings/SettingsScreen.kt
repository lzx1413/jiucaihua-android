package com.jiucaihua.app.presentation.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.R
import com.jiucaihua.app.ai.model.AiProvider
import com.jiucaihua.app.ai.model.metadata
import com.jiucaihua.app.BuildConfig
import com.jiucaihua.app.data.backup.BackupRepository
import com.jiucaihua.app.i18n.AppLocaleManager

private const val SHOW_AI_SETTINGS = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showResetReferenceDateDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                backupViewModel.completeExport(uri)
            } else {
                backupViewModel.cancelExport()
            }
        },
    )

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedRestoreUri = uri
                showRestoreDialog = true
            } else {
                backupViewModel.cancelImport()
            }
        },
    )

    if (showRestoreDialog && selectedRestoreUri != null) {
        RestoreModeDialog(
            onDismiss = {
                showRestoreDialog = false
                selectedRestoreUri = null
                backupViewModel.cancelImport()
            },
            onConfirm = { mode ->
                showRestoreDialog = false
                backupViewModel.completeImport(selectedRestoreUri!!, mode)
                selectedRestoreUri = null
            },
        )
    }

    if (showResetReferenceDateDialog) {
        AlertDialog(
            onDismissRequest = { showResetReferenceDateDialog = false },
            title = { Text(stringResource(R.string.reset_reference_date)) },
            text = { Text(stringResource(R.string.reset_reference_date_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetPortfolioReferenceDate()
                    showResetReferenceDateDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetReferenceDateDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
            SectionHeader(stringResource(R.string.settings_refresh))
            RefreshIntervalSelector(
                currentInterval = uiState.refreshIntervalSeconds,
                onSelect = { viewModel.setRefreshInterval(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_portfolio))
            Button(
                onClick = { showResetReferenceDateDialog = true },
                enabled = !uiState.isResettingReferenceDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.reset_reference_date))
            }
            uiState.referenceDateResetMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_language))
            LanguageSelector(
                selectedLanguage = uiState.languageTag,
                onSelect = { viewModel.setLanguage(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_appearance))
            ThemeSelector(
                isDarkMode = uiState.isDarkMode,
                onSelect = { viewModel.setDarkMode(it) },
            )
            if (uiState.isDarkMode != false) {
                SettingSwitch(
                    title = stringResource(R.string.settings_oled_mode),
                    subtitle = stringResource(R.string.settings_oled_mode_subtitle),
                    checked = uiState.oledMode,
                    onCheckedChange = { viewModel.setOledMode(it) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_notifications))
            SettingSwitch(
                title = stringResource(R.string.settings_alert_notifications),
                subtitle = stringResource(R.string.settings_alert_notifications_subtitle),
                checked = uiState.alertsEnabled,
                onCheckedChange = { viewModel.setAlertsEnabled(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (SHOW_AI_SETTINGS) {
                SectionHeader(stringResource(R.string.settings_ai))
                AiProviderSelector(
                    selectedProvider = uiState.aiConfig.provider,
                    onSelect = { viewModel.setAiProvider(it) },
                )
                SettingTextField(
                    title = "API Key",
                    value = uiState.aiConfig.apiKey,
                    onValueChange = viewModel::setAiApiKey,
                    placeholder = stringResource(R.string.settings_api_key_placeholder),
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
                    title = stringResource(R.string.settings_model),
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
                        Text(
                            if (uiState.isLoadingModels) {
                                stringResource(R.string.settings_loading_models)
                            } else {
                                stringResource(R.string.settings_fetch_models)
                            }
                        )
                    }
                    Button(
                        onClick = viewModel::testAiConnection,
                        enabled = !uiState.isTestingConnection && uiState.aiConfig.apiKey.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (uiState.isTestingConnection) {
                                stringResource(R.string.settings_testing_connection)
                            } else {
                                stringResource(R.string.settings_test_connection)
                            }
                        )
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
                    title = stringResource(R.string.settings_enable_thinking),
                    subtitle = stringResource(R.string.settings_enable_thinking_subtitle),
                    checked = uiState.aiConfig.enableThinking,
                    onCheckedChange = { viewModel.setAiThinkingEnabled(it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            SectionHeader(stringResource(R.string.settings_backup))
            if (backupState.isExporting || backupState.isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            backupState.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (backupState.isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { backupViewModel.exportData(createDocumentLauncher) },
                    enabled = !backupState.isExporting && !backupState.isImporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_export_data))
                }
                Button(
                    onClick = { backupViewModel.startImport(openDocumentLauncher) },
                    enabled = !backupState.isExporting && !backupState.isImporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_import_data))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_about))
            SettingItem(title = stringResource(R.string.settings_version), subtitle = BuildConfig.VERSION_NAME)
            SettingItem(title = stringResource(R.string.settings_app_name), subtitle = stringResource(R.string.app_name))
            SettingItem(title = "GitHub", subtitle = BuildConfig.GITHUB_URL)
            SettingItem(title = stringResource(R.string.settings_build_date), subtitle = BuildConfig.BUILD_DATE)
        }
    }
}

@Composable
private fun RestoreModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (BackupRepository.RestoreMode) -> Unit,
) {
    var selectedMode by remember { mutableStateOf(BackupRepository.RestoreMode.REPLACE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_mode)) },
        text = {
            Column {
                Text(stringResource(R.string.restore_mode_prompt), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = BackupRepository.RestoreMode.REPLACE }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedMode == BackupRepository.RestoreMode.REPLACE,
                        onClick = { selectedMode = BackupRepository.RestoreMode.REPLACE },
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.restore_replace), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.restore_replace_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = BackupRepository.RestoreMode.MERGE }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedMode == BackupRepository.RestoreMode.MERGE,
                        onClick = { selectedMode = BackupRepository.RestoreMode.MERGE },
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.restore_merge), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.restore_merge_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode) }) {
                Text(stringResource(R.string.restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
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
    val options = listOf(
        5 to stringResource(R.string.interval_5_seconds),
        10 to stringResource(R.string.interval_10_seconds),
        15 to stringResource(R.string.interval_15_seconds),
        30 to stringResource(R.string.interval_30_seconds),
    )
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
        null to stringResource(R.string.theme_system),
        false to stringResource(R.string.theme_light),
        true to stringResource(R.string.theme_dark),
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
private fun LanguageSelector(
    selectedLanguage: String,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        AppLocaleManager.LANGUAGE_SYSTEM to stringResource(R.string.language_system),
        AppLocaleManager.LANGUAGE_ZH to stringResource(R.string.language_chinese),
        AppLocaleManager.LANGUAGE_EN to stringResource(R.string.language_english),
    )
    options.forEach { (languageTag, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(languageTag) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedLanguage == languageTag,
                onClick = { onSelect(languageTag) },
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
