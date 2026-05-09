package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
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
    var showLossDialog by remember { mutableStateOf(false) }
    var lossInputText by remember { mutableStateOf("") }

    if (showCashDialog) {
        AlertDialog(
            onDismissRequest = { showCashDialog = false },
            title = { Text("设置现金") },
            text = {
                OutlinedTextField(
                    value = cashInputText,
                    onValueChange = { cashInputText = it },
                    label = { Text("现金金额（元）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    cashInputText.toDoubleOrNull()?.let { onSetCash(it) }
                    showCashDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCashDialog = false }) { Text("取消") }
            }
        )
    }

    if (showLossDialog) {
        AlertDialog(
            onDismissRequest = { showLossDialog = false },
            title = { Text("亏损补偿") },
            text = {
                Column {
                    Text(
                        text = "填写历史已实现亏损总额，用于计算累计收益",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = lossInputText,
                        onValueChange = { lossInputText = it },
                        label = { Text("亏损金额（元）") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    lossInputText.toDoubleOrNull()?.let { onSetLossCompensation(it) }
                    showLossDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showLossDialog = false }) { Text("取消") }
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
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "总资产",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (totalAssets > 0) formatMoney(totalAssets) else "--",
                style = MaterialTheme.typography.headlineMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "持仓收益",
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
                        text = "累计收益",
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
                        text = "今日",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val todayColor = when {
                        summary.todayEarnings > 0 -> RiseRed
                        summary.todayEarnings < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (totalAssets > 0) formatSignedMoney(summary.todayEarnings) else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = todayColor
                    )
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
                        text = "持仓市值",
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
                        text = "现金",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val cashColor = when {
                        summary.cash > 0 -> FallGreen
                        summary.cash < 0 -> RiseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (summary.cash > 0) formatMoney(summary.cash) else "点击设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (summary.cash > 0) cashColor else MaterialTheme.colorScheme.primary,
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
                        text = "亏损补偿",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (summary.lossCompensation > 0) formatMoney(summary.lossCompensation) else "点击设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (summary.lossCompensation > 0) RiseRed else MaterialTheme.colorScheme.primary,
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
