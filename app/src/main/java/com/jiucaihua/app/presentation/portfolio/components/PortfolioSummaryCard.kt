package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    onSetCash: (Double) -> Unit = {},
    onSetLossCompensation: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCashDialog by remember { mutableStateOf(false) }
    var cashInputText by remember { mutableStateOf("") }
    var cashDiffText by remember { mutableStateOf("") }
    var showLossDialog by remember { mutableStateOf(false) }
    var lossInputText by remember { mutableStateOf("") }

    if (showCashDialog) {
        val currentCash = summary.cash
        val diffValue = cashDiffText.toDoubleOrNull() ?: 0.0
        val computedCash = currentCash + diffValue
        if (diffValue != 0.0) {
            cashInputText = computedCash.toLong().toString()
        }

        AlertDialog(
            onDismissRequest = { showCashDialog = false },
            title = { Text(stringResource(R.string.set_cash)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cashInputText,
                        onValueChange = {
                            cashInputText = it
                            cashDiffText = ""
                        },
                        label = { Text(stringResource(R.string.cash_amount_yuan)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = cashDiffText,
                        onValueChange = { cashDiffText = it },
                        label = { Text(stringResource(R.string.cash_delta_amount)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    cashInputText.toDoubleOrNull()?.let { onSetCash(it) }
                    showCashDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showCashDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showLossDialog) {
        AlertDialog(
            onDismissRequest = { showLossDialog = false },
            title = { Text(stringResource(R.string.loss_compensation)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.loss_compensation_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = lossInputText,
                        onValueChange = { lossInputText = it },
                        label = { Text(stringResource(R.string.loss_amount_yuan)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    lossInputText.toDoubleOrNull()?.let { onSetLossCompensation(it) }
                    showLossDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLossDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    val totalAssets = summary.totalMarketValue + summary.cash

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.portfolio_summary_total_assets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (totalAssets > 0) formatMoney(totalAssets) else "--",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.portfolio_summary_holding_earnings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val earningsColor = when {
                        summary.totalEarnings > 0 -> RiseRed
                        summary.totalEarnings < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (totalAssets > 0) {
                            "${formatSignedMoney(summary.totalEarnings)} (${formatPercent(summary.totalEarningsPercent)})"
                        } else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = earningsColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.portfolio_summary_total_earnings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val cumulativeColor = when {
                        summary.cumulativeEarnings > 0 -> RiseRed
                        summary.cumulativeEarnings < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (totalAssets > 0) {
                            "${formatSignedMoney(summary.cumulativeEarnings)} (${formatPercent(summary.cumulativeEarningsPercent)})"
                        } else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = cumulativeColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.portfolio_summary_today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val todayColor = when {
                        summary.todayEarnings > 0 -> MaterialTheme.colorScheme.tertiary
                        summary.todayEarnings < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (summary.todayEarnings != 0.0 && totalAssets > 0) {
                            Icon(
                                imageVector = if (summary.todayEarnings > 0)
                                    Icons.Filled.KeyboardArrowUp
                                else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = todayColor,
                            )
                        }
                        Text(
                            text = if (totalAssets > 0) formatSignedMoney(summary.todayEarnings) else "--",
                            style = MaterialTheme.typography.titleMedium,
                            color = todayColor,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.holding_market_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (summary.totalMarketValue > 0) formatMoney(summary.totalMarketValue) else "--",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.cash),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (summary.cash > 0) formatMoney(summary.cash) else stringResource(R.string.tap_to_set),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (summary.cash > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            cashInputText = if (summary.cash > 0) summary.cash.toLong().toString() else ""
                            showCashDialog = true
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.loss_compensation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (summary.lossCompensation > 0) {
                            formatMoney(summary.lossCompensation)
                        } else {
                            stringResource(R.string.tap_to_set)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (summary.lossCompensation > 0) FallGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            lossInputText = if (summary.lossCompensation > 0) summary.lossCompensation.toLong().toString() else ""
                            showLossDialog = true
                        }
                    )
                }
            }
        }
    }
}

private fun formatMoney(value: Double): String {
    return "¥%,.2f".format(value)
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign¥%,.2f".format(value)
}

private fun formatPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign%.2f%%".format(value)
}
