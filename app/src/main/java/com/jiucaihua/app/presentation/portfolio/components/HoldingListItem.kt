package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoldingListItem(
    holding: Holding,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val hasQuote = holding.currentPrice > 0

            // Row 1: Name | Daily P&L Amount | Current Price | Cumulative P&L Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = holding.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2.5f),
                )
                if (hasQuote) {
                    PnLWithArrow(
                        text = formatSignedMoney(holding.dailyEarningsCNY),
                        value = holding.dailyEarningsCNY,
                        modifier = Modifier.weight(1.5f),
                    )
                    Text(
                        text = "%.2f".format(holding.currentPrice),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.End,
                    )
                    PnLWithArrow(
                        text = formatSignedMoney(holding.earningsCNY),
                        value = holding.earningsCNY,
                        modifier = Modifier.weight(1.5f),
                    )
                }
            }

            // Row 2: Holding Amount | Daily P&L % | Cost | Cumulative P&L %
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val holdingAmountValue = if (hasQuote) holding.marketValueCNY
                    else holding.holdingAmount * holding.exchangeRate
                Text(
                    text = if (holdingAmountValue > 0) formatMoney(holdingAmountValue) else "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(2.5f),
                )
                if (hasQuote) {
                    PnLWithArrow(
                        text = formatChangePercent(holding.changePercent),
                        value = holding.changePercent,
                        modifier = Modifier.weight(1.5f),
                    )
                    Text(
                        text = if (holding.costPrice > 0) "%.2f".format(holding.costPrice) else "--",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.End,
                    )
                    PnLWithArrow(
                        text = formatChangePercent(holding.earningsPercent),
                        value = holding.earningsPercent,
                        modifier = Modifier.weight(1.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PnLWithArrow(
    text: String,
    value: Double,
    modifier: Modifier = Modifier,
) {
    val color = pLColor(value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (value != 0.0) {
            Icon(
                imageVector = if (value > 0)
                    Icons.Filled.KeyboardArrowUp
                else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun pLColor(value: Double) = when {
    value > 0 -> RiseRed
    value < 0 -> FallGreen
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign¥%,.0f".format(value)
}

private fun formatMoney(value: Double): String {
    return "¥%,.0f".format(value)
}

private fun formatChangePercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign%.2f%%".format(value)
}
