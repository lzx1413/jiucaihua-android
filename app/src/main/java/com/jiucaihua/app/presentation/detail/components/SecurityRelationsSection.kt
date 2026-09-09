package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.SecurityRelationsSnapshot
import java.util.Locale

@Composable
fun SecurityRelationsSection(snapshot: SecurityRelationsSnapshot, modifier: Modifier = Modifier) {
    if (snapshot.plates.isEmpty() && snapshot.relatedSecurities.isEmpty() && snapshot.shareholders.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("资料", style = MaterialTheme.typography.titleMedium)
            if (snapshot.plates.isNotEmpty()) {
                Text("所属板块", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow { snapshot.plates.take(12).forEach { Text(it.name, modifier = Modifier.padding(end = 10.dp, top = 4.dp), style = MaterialTheme.typography.bodyMedium) } }
            }
            if (snapshot.relatedSecurities.isNotEmpty()) {
                Text("关联证券", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow { snapshot.relatedSecurities.take(10).forEach { Text(it.name, modifier = Modifier.padding(end = 10.dp, top = 4.dp), style = MaterialTheme.typography.bodyMedium) } }
            }
            if (snapshot.shareholders.isNotEmpty()) {
                Text(
                    "前十大流通股东${snapshot.shareholderReportPeriod?.let { "（$it）" }.orEmpty()}",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                snapshot.shareholders.take(10).forEach { shareholder ->
                    Text(
                        "${shareholder.name}  ${shareholder.floatPercent?.let { String.format(Locale.ROOT, "%.2f%%", it) } ?: "--"}",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
