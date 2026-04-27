package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    modifier: Modifier = Modifier
) {
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
