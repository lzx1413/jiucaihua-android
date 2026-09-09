package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.StockFundFlowSnapshot
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import java.util.Locale

@Composable
fun StockFundFlowCard(snapshot: StockFundFlowSnapshot, modifier: Modifier = Modifier) {
    if (snapshot.mainNet == null && snapshot.retailNet == null) return
    val locale = LocalLocale.current.platformLocale as? Locale ?: Locale.ROOT
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("个股资金流", style = MaterialTheme.typography.titleMedium)
            Text("单位：${snapshot.unit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FlowValue("主力", snapshot.mainNet, locale, Modifier.weight(1f))
                FlowValue("散户", snapshot.retailNet, locale, Modifier.weight(1f))
                FlowValue("超大单", snapshot.superLargeNet, locale, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlowValue(label: String, value: Double?, locale: Locale, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value?.let { String.format(locale, "%+.2f", it) } ?: "--",
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                value == null -> MaterialTheme.colorScheme.onSurfaceVariant
                value >= 0 -> RiseRed
                else -> FallGreen
            },
        )
    }
}
