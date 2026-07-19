package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.ReturnHistoryResult
import com.jiucaihua.app.domain.model.ReturnHistoryType
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun ReturnHistoryDialog(
    result: ReturnHistoryResult,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (result.type) {
                    ReturnHistoryType.DAILY -> stringResource(R.string.return_history_daily)
                    ReturnHistoryType.MONTHLY -> stringResource(R.string.return_history_monthly)
                    ReturnHistoryType.YEARLY -> stringResource(R.string.return_history_yearly)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.selectorOptions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(result.selectorOptions) { option ->
                            FilterChip(
                                selected = option == result.selectedOption,
                                onClick = { onOptionSelected(option) },
                                label = { Text(option) },
                            )
                        }
                    }
                }
                if (result.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.return_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(result.items, key = { it.label }) { item ->
                            val color = when {
                                item.earnings > 0 -> RiseRed
                                item.earnings < 0 -> FallGreen
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatReturn(item.earnings, item.earningsPercent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

private fun formatReturn(earnings: Double, percent: Double): String {
    val sign = if (earnings >= 0) "+" else ""
    return "$sign¥%,.2f (%s%.2f%%)".format(earnings, if (percent >= 0) "+" else "", percent)
}
