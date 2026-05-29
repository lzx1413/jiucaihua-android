package com.jiucaihua.app.presentation.alerts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.PriceAlert

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlertDialog(
    alert: PriceAlert,
    holdings: List<Holding>,
    onConfirm: (id: Long, code: String, name: String, alertType: AlertType, threshold: Double, actionHint: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Pre-populate with existing alert data
    var selectedHolding by remember {
        mutableStateOf(holdings.firstOrNull { it.code == alert.code })
    }
    var selectedAlertType by remember { mutableStateOf(alert.alertType) }
    var thresholdText by remember { mutableStateOf(alert.threshold.toString()) }
    var actionHintText by remember { mutableStateOf(alert.actionHint ?: "") }
    var holdingDropdownExpanded by remember { mutableStateOf(false) }

    val isValid = selectedHolding != null && thresholdText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑预警") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = holdingDropdownExpanded,
                    onExpandedChange = { holdingDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedHolding?.let { "${it.name} (${it.code})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择证券") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = holdingDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = holdingDropdownExpanded,
                        onDismissRequest = { holdingDropdownExpanded = false },
                    ) {
                        holdings.forEach { holding ->
                            DropdownMenuItem(
                                text = { Text("${holding.name} (${holding.code})") },
                                onClick = {
                                    selectedHolding = holding
                                    holdingDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Text(
                    text = "预警类型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AlertType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedAlertType == type,
                            onClick = { selectedAlertType = type },
                            label = { Text(type.label) },
                        )
                    }
                }

                val thresholdLabel = when (selectedAlertType) {
                    AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> "阈值价格"
                    AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> "阈值百分比 (%)"
                }
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text(thresholdLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = actionHintText,
                    onValueChange = { actionHintText = it },
                    label = { Text("操作提示（如：加仓500股）") },
                    placeholder = { Text("可选，触发时的操作建议") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holding = selectedHolding ?: return@TextButton
                    val threshold = thresholdText.toDoubleOrNull() ?: return@TextButton
                    val actionHint = actionHintText.takeIf { it.isNotBlank() }
                    onConfirm(alert.id, holding.code, holding.name, selectedAlertType, threshold, actionHint)
                },
                enabled = isValid,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}