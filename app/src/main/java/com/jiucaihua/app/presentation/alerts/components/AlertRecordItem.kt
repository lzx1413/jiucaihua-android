package com.jiucaihua.app.presentation.alerts.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.AlertType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertRecordItem(
    record: AlertRecord,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${record.name} (${record.code})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTriggerTime(record.triggeredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatRecordDetail(record),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.actionHint != null) {
                Text(
                    text = "💡 ${record.actionHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatTriggerTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRecordDetail(record: AlertRecord): String {
    val suffix = when (record.alertType) {
        AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> ""
        AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW -> "%"
    }
    val currentSuffix = suffix
    return "${record.alertType.label} ${record.threshold}$suffix，触发值 ${record.currentValue}$currentSuffix"
}
