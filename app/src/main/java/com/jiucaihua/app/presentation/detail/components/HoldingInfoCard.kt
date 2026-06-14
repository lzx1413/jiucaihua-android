package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.BorderStroke
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
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun HoldingInfoCard(
    holding: Holding,
    modifier: Modifier = Modifier,
) {
    val earningsColor = when {
        holding.earningsCNY > 0 -> RiseRed
        holding.earningsCNY < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val currencyPrefix = when (holding.marketType) {
        MarketType.HK_STOCK -> "HK$"
        else -> "¥"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我的持仓",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoItem("成本价", "$currencyPrefix%.2f".format(holding.costPrice))
                val sharesLabel = if (holding.marketType == MarketType.FUND) "份额" else "股数"
                val sharesText = if (holding.marketType == MarketType.FUND) {
                    "%.2f份".format(holding.holdingShares)
                } else {
                    "%.0f股".format(holding.holdingShares)
                }
                InfoItem(sharesLabel, sharesText)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoItem("市值", "¥%,.2f".format(holding.marketValueCNY))
                InfoItem(
                    label = "盈亏",
                    value = formatSignedMoney(holding.earningsCNY),
                    valueColor = earningsColor,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val sign = if (holding.earningsPercent >= 0) "+" else ""
                InfoItem(
                    label = "收益率",
                    value = "$sign%.2f%%".format(holding.earningsPercent),
                    valueColor = earningsColor,
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
        )
    }
}

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign¥%,.2f".format(value)
}
