package com.jiucaihua.app.presentation.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.presentation.alerts.components.AddAlertDialog
import com.jiucaihua.app.presentation.alerts.components.AlertListItem
import com.jiucaihua.app.presentation.alerts.components.AlertRecordItem
import com.jiucaihua.app.presentation.alerts.components.EditAlertDialog
import com.jiucaihua.app.presentation.common.components.EmptyState
import com.jiucaihua.app.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf("预警设置", "触发记录")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预警管理") },
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
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加预警",
                    )
                }
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                        )
                    }
                }
                when (selectedTabIndex) {
                    0 -> AlertSettingsTab(
                        alerts = uiState.alerts,
                        onToggle = { id, enabled -> viewModel.toggleAlert(id, enabled) },
                        onEdit = { alert -> viewModel.showEditDialog(alert) },
                        onDelete = { id -> viewModel.deleteAlert(id) },
                    )
                    1 -> AlertRecordsTab(records = uiState.records)
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddAlertDialog(
            holdings = uiState.holdings,
            onConfirm = { code, name, type, threshold, actionHint ->
                viewModel.addAlert(code, name, type, threshold, actionHint)
            },
            onDismiss = { viewModel.hideAddDialog() },
        )
    }

    val editingAlert = uiState.editingAlert
    if (editingAlert != null) {
        EditAlertDialog(
            alert = editingAlert,
            holdings = uiState.holdings,
            onConfirm = { id, code, name, type, threshold, actionHint ->
                viewModel.updateAlert(id, code, name, type, threshold, actionHint)
            },
            onDismiss = { viewModel.hideEditDialog() },
        )
    }
}

@Composable
private fun AlertSettingsTab(
    alerts: List<PriceAlert>,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (PriceAlert) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (alerts.isEmpty()) {
        EmptyState(message = "暂无预警规则")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = alerts, key = { it.id }) { alert ->
                AlertListItem(
                    alert = alert,
                    onToggle = { onToggle(alert.id, it) },
                    onEdit = { onEdit(alert) },
                    onDelete = { onDelete(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun AlertRecordsTab(
    records: List<AlertRecord>,
) {
    if (records.isEmpty()) {
        EmptyState(message = "暂无触发记录", hint = "")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = records, key = { it.id }) { record ->
                AlertRecordItem(record = record)
            }
        }
    }
}
