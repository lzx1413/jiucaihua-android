package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.SecurityEvent
import com.jiucaihua.app.domain.model.SecurityEventKind
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SecurityEventsSection(
    title: String,
    events: List<SecurityEvent>,
    isLoading: Boolean,
    error: String?,
    onEventClick: (SecurityEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedKind by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = events.filter { selectedKind == null || it.kind.name == selectedKind }
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            EventKindFilters(selectedKind = selectedKind, onKindSelected = { selectedKind = it })
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                filtered.isEmpty() -> Text("暂无相关资讯", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                else -> filtered.forEachIndexed { index, event ->
                    SecurityEventItem(event, onEventClick)
                    if (index != filtered.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventKindFilters(selectedKind: String?, onKindSelected: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EventKindChip("全部", selectedKind == null) { onKindSelected(null) }
        EventKindChip("新闻", selectedKind == SecurityEventKind.NEWS.name) { onKindSelected(SecurityEventKind.NEWS.name) }
        EventKindChip("公告", selectedKind == SecurityEventKind.ANNOUNCEMENT.name) { onKindSelected(SecurityEventKind.ANNOUNCEMENT.name) }
        EventKindChip("报告", selectedKind == SecurityEventKind.PERIODIC_REPORT.name) { onKindSelected(SecurityEventKind.PERIODIC_REPORT.name) }
        EventKindChip("研报", selectedKind == SecurityEventKind.RESEARCH.name) { onKindSelected(SecurityEventKind.RESEARCH.name) }
    }
}

@Composable
private fun EventKindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SecurityEventItem(event: SecurityEvent, onEventClick: (SecurityEvent) -> Unit) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", LocalLocale.current.platformLocale)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = { onEventClick(event) })
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                event.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = if (event.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            listOf(event.publisher.ifBlank { "腾讯聚合" }, if (event.publishedAt > 0) formatter.format(Date(event.publishedAt)) else "时间未知")
                .joinToString("  "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (event.summary.isNotBlank()) {
            Text(event.summary, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
