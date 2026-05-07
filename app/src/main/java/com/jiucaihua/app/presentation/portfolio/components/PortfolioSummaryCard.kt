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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    onSetTotalPosition: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("设置总仓位") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("总仓位金额（元）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    inputText.toDoubleOrNull()?.let { onSetTotalPosition(it) }
                    showDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }

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
                text = if (summary.totalMarketValue > 0) formatMoney(summary.totalMarketValue) else "--",
                style = MaterialTheme.typography.headlineMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "总收益",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val earningsColor = when {
                        summary.totalEarnings > 0 -> RiseRed
                        summary.totalEarnings < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (summary.totalMarketValue > 0) {
                            "${formatSignedMoney(summary.totalEarnings)} (${formatPercent(summary.totalEarningsPercent)})"
                        } else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = earningsColor
                    )
                }
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
                        text = if (summary.totalMarketValue > 0) formatSignedMoney(summary.todayEarnings) else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = todayColor
                    )
                }
            }

            if (summary.totalPosition > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "总仓位",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(summary.totalPosition),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable {
                                inputText = summary.totalPosition.toLong().toString()
                                showDialog = true
                            }
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
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
                            text = formatMoney(summary.cash),
                            style = MaterialTheme.typography.titleMedium,
                            color = cashColor,
                            modifier = Modifier.clickable {
                                inputText = summary.totalPosition.toLong().toString()
                                showDialog = true
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable {
                            inputText = ""
                            showDialog = true
                        }
                ) {
                    Text(
                        text = "点击设置总仓位",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
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
