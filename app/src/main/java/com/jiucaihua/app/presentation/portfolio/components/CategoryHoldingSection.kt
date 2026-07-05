package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.presentation.i18n.localizedLabel
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import java.util.Locale

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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
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
                        text = categorySummary.marketType.localizedLabel(),
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
                        label = stringResource(R.string.market_value),
                        value = formatMoney(categorySummary.totalMarketValue),
                        color = MaterialTheme.colorScheme.onSurface,
                        width = 90.dp,
                    )
                    StatColumn(
                        label = stringResource(R.string.earnings),
                        value = formatSignedMoney(categorySummary.totalEarnings),
                        subValue = formatPercent(categorySummary.totalEarningsPercent),
                        color = getValueColor(categorySummary.totalEarnings),
                        width = 100.dp,
                    )
                    StatColumn(
                        label = stringResource(R.string.today),
                        value = formatSignedMoney(categorySummary.todayEarnings),
                        color = getValueColor(categorySummary.todayEarnings),
                        width = 80.dp,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.action_collapse)
                        } else {
                            stringResource(R.string.action_expand)
                        },
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
    if (!isChineseLocale()) {
        return when {
            value >= 1_000_000 -> "¥%.1fM".format(value / 1_000_000)
            value >= 1_000 -> "¥%.1fK".format(value / 1_000)
            else -> "¥%.0f".format(value)
        }
    }
    if (value >= 10000) {
        return "¥%.1f万".format(value / 10000)
    }
    return "¥%.0f".format(value)
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    val absValue = kotlin.math.abs(value)
    if (!isChineseLocale()) {
        return when {
            absValue >= 1_000_000 -> "$sign¥%.1fM".format(absValue / 1_000_000)
            absValue >= 1_000 -> "$sign¥%.1fK".format(absValue / 1_000)
            else -> "$sign¥%.0f".format(absValue)
        }
    }
    if (absValue >= 10000) {
        return "$sign¥%.1f万".format(absValue / 10000)
    }
    return "$sign¥%.0f".format(absValue)
}

private fun formatPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign%.2f%%".format(value)
}

private fun isChineseLocale(): Boolean = Locale.getDefault().language == Locale.CHINESE.language
