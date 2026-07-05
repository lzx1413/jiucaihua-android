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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.presentation.i18n.localizedLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAlertDialog(
    holdings: List<Holding>,
    onConfirm: (code: String, name: String, alertType: AlertType, threshold: Double, actionHint: String?, params: Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHolding by remember { mutableStateOf<Holding?>(null) }
    var selectedAlertType by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var thresholdText by remember { mutableStateOf("") }
    var actionHintText by remember { mutableStateOf("") }
    var periodText by remember { mutableStateOf("20") }
    var shortPeriodText by remember { mutableStateOf("5") }
    var longPeriodText by remember { mutableStateOf("20") }
    var holdingDropdownExpanded by remember { mutableStateOf(false) }

    val needsThreshold = selectedAlertType in setOf(
        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW,
        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW,
        AlertType.VOLUME_ABOVE,
    )
    val needsPeriod = selectedAlertType in setOf(AlertType.NEW_HIGH, AlertType.NEW_LOW)
    val needsMAPeriods = selectedAlertType in setOf(AlertType.MA_CROSS_ABOVE, AlertType.MA_CROSS_BELOW)

    val isValid = selectedHolding != null && when {
        needsThreshold -> thresholdText.toDoubleOrNull() != null
        needsPeriod -> periodText.toIntOrNull() != null && periodText.toInt() >= 5
        needsMAPeriods -> shortPeriodText.toIntOrNull() != null && longPeriodText.toIntOrNull() != null &&
            shortPeriodText.toInt() >= 2 && longPeriodText.toInt() >= 5
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_alert)) },
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
                        label = { Text(stringResource(R.string.select_security)) },
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
                    text = stringResource(R.string.alert_type),
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
                            label = { Text(type.localizedLabel()) },
                        )
                    }
                }

                if (needsThreshold) {
                    val thresholdLabel = when (selectedAlertType) {
                        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> stringResource(R.string.threshold_price)
                        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> stringResource(R.string.threshold_percent)
                        AlertType.VOLUME_ABOVE -> stringResource(R.string.threshold_volume)
                        else -> stringResource(R.string.threshold)
                    }
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it },
                        label = { Text(thresholdLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (needsPeriod) {
                    OutlinedTextField(
                        value = periodText,
                        onValueChange = { periodText = it },
                        label = { Text(stringResource(R.string.days_n_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (needsMAPeriods) {
                    OutlinedTextField(
                        value = shortPeriodText,
                        onValueChange = { shortPeriodText = it },
                        label = { Text(stringResource(R.string.short_period_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = longPeriodText,
                        onValueChange = { longPeriodText = it },
                        label = { Text(stringResource(R.string.long_period_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = actionHintText,
                    onValueChange = { actionHintText = it },
                    label = { Text(stringResource(R.string.action_hint_label)) },
                    placeholder = { Text(stringResource(R.string.action_hint_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holding = selectedHolding ?: return@TextButton
                    val threshold = when {
                        needsThreshold -> thresholdText.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val params = when {
                        needsPeriod -> mapOf("period" to periodText)
                        needsMAPeriods -> mapOf("short_period" to shortPeriodText, "long_period" to longPeriodText)
                        else -> emptyMap()
                    }
                    val actionHint = actionHintText.takeIf { it.isNotBlank() }
                    onConfirm(holding.code, holding.name, selectedAlertType, threshold, actionHint, params)
                },
                enabled = isValid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
