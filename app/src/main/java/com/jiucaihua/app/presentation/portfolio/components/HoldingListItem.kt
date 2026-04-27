package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holding.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = holding.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val priceText = if (holding.currentPrice > 0) {
                    val prefix = when (holding.marketType) {
                        MarketType.HK_STOCK -> "HK$"
                        else -> "¥"
                    }
                    "$prefix%.2f".format(holding.currentPrice)
                } else "--"

                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleMedium,
                )

                if (holding.currentPrice > 0) {
                    val changeColor = when {
                        holding.changePercent > 0 -> RiseRed
                        holding.changePercent < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = formatChangePercent(holding.changePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor
                    )
                }
            }

            if (holding.currentPrice > 0) {
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val earningsColor = when {
                        holding.earningsCNY > 0 -> RiseRed
                        holding.earningsCNY < 0 -> FallGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = formatSignedMoney(holding.earningsCNY),
                        style = MaterialTheme.typography.bodyMedium,
                        color = earningsColor,
                    )
                    Text(
                        text = formatChangePercent(holding.earningsPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = earningsColor,
                    )
                }
            }
        }
    }
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign¥%,.2f".format(value)
}

private fun formatChangePercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign%.2f%%".format(value)
}
