package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun CategoryHoldingSection(
    categorySummary: CategorySummary,
    onHoldingClick: (String) -> Unit,
    onHoldingLongClick: (Holding) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categorySummary.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = " (${categorySummary.holdings.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatColumn(
                        label = "市值",
                        value = formatMoney(categorySummary.totalMarketValue),
                        color = MaterialTheme.colorScheme.onSurface,
                        width = 90.dp,
                    )
                    StatColumn(
                        label = "收益",
                        value = formatSignedMoney(categorySummary.totalEarnings),
                        subValue = formatPercent(categorySummary.totalEarningsPercent),
                        color = getValueColor(categorySummary.totalEarnings),
                        width = 100.dp,
                    )
                    StatColumn(
                        label = "今日",
                        value = formatSignedMoney(categorySummary.todayEarnings),
                        color = getValueColor(categorySummary.todayEarnings),
                        width = 80.dp,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    categorySummary.holdings.forEach { holding ->
                        HoldingListItem(
                            holding = holding,
                            onClick = { onHoldingClick(holding.code) },
                            onLongClick = { onHoldingLongClick(holding) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    subValue: String? = null,
    color: androidx.compose.ui.graphics.Color,
    width: androidx.compose.ui.unit.Dp,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.width(width),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        if (subValue != null) {
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun getValueColor(value: Double): androidx.compose.ui.graphics.Color {
    return when {
        value > 0 -> RiseRed
        value < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
}

private fun formatMoney(value: Double): String {
    if (value >= 10000) {
        return "¥%.1f万".format(value / 10000)
    }
    return "¥%.0f".format(value)
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    val absValue = kotlin.math.abs(value)
    if (absValue >= 10000) {
        return "$sign¥%.1f万".format(absValue / 10000)
    }
    return "$sign¥%.0f".format(absValue)
}

private fun formatPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign%.2f%%".format(value)
}