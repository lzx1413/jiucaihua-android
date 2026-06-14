package com.jiucaihua.app.presentation.alerts.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.PriceAlert

@Composable
fun AlertListItem(
    alert: PriceAlert,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${alert.name} (${alert.code})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatAlertCondition(alert),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (alert.actionHint != null) {
                    Text(
                        text = "💡 ${alert.actionHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = alert.isEnabled,
                onCheckedChange = onToggle,
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑预警",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除预警",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatAlertCondition(alert: PriceAlert): String {
    val suffix = when (alert.alertType) {
        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> ""
        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> "%"
        AlertType.VOLUME_ABOVE -> "手"
        AlertType.NEW_HIGH, AlertType.NEW_LOW -> {
            val period = alert.params["period"] ?: "20"
            "(${period}日)"
        }
        AlertType.MA_CROSS_ABOVE, AlertType.MA_CROSS_BELOW -> {
            val shortPeriod = alert.params["short_period"] ?: "5"
            val longPeriod = alert.params["long_period"] ?: "20"
            "(MA${shortPeriod}/MA${longPeriod})"
        }
    }
    return "${alert.alertType.label} ${alert.threshold}$suffix"
}
