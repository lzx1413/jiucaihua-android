package com.jiucaihua.app.presentation.market.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.FundFlowData
import com.jiucaihua.app.domain.model.NorthFlowData
import com.jiucaihua.app.domain.model.SouthFlowData
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import java.util.Locale

@Composable
fun FundFlowCard(
    fundFlowData: FundFlowData,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.fund_flow),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(24.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    FundFlowItem(
                        title = stringResource(R.string.northbound_funds),
                        totalFlow = fundFlowData.northFlow.totalNetInflow,
                        hgtFlow = fundFlowData.northFlow.hgtNetInflow,
                        sgtFlow = fundFlowData.northFlow.sgtNetInflow,
                        hgtLabel = stringResource(R.string.shanghai_stock_connect),
                        sgtLabel = stringResource(R.string.shenzhen_stock_connect),
                    )

                    FundFlowItem(
                        title = stringResource(R.string.southbound_funds),
                        totalFlow = fundFlowData.southFlow.totalNetInflow,
                        hgtFlow = fundFlowData.southFlow.ggtShNetInflow,
                        sgtFlow = fundFlowData.southFlow.ggtSzNetInflow,
                        hgtLabel = stringResource(R.string.hk_connect_shanghai),
                        sgtLabel = stringResource(R.string.hk_connect_shenzhen),
                    )
                }
            }
        }
    }
}

@Composable
private fun FundFlowItem(
    title: String,
    totalFlow: Double,
    hgtFlow: Double,
    sgtFlow: Double,
    hgtLabel: String,
    sgtLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val totalColor = when {
            totalFlow > 0 -> RiseRed
            totalFlow < 0 -> FallGreen
            else -> MaterialTheme.colorScheme.onSurface
        }

        val sign = if (totalFlow >= 0) "+" else ""

        Text(
            text = formatSignedFlowAmount(totalFlow),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = totalColor,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FlowDetailItem(label = hgtLabel, flow = hgtFlow)
            FlowDetailItem(label = sgtLabel, flow = sgtFlow)
        }
    }
}

@Composable
private fun FlowDetailItem(
    label: String,
    flow: Double,
    modifier: Modifier = Modifier,
) {
    val color = when {
        flow > 0 -> RiseRed
        flow < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sign = if (flow >= 0) "+" else ""

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatSignedFlowAmount(flow),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

private fun formatFlowAmount(amount: Double): String {
    val billionAmount = amount / 10000.0
    return "%.2f".format(billionAmount)
}

private fun formatSignedFlowAmount(amount: Double): String {
    val sign = if (amount >= 0) "+" else ""
    val value = formatFlowAmount(amount)
    return if (Locale.getDefault().language == Locale.CHINESE.language) {
        "$sign${value}亿"
    } else {
        "$sign${value}B"
    }
}
