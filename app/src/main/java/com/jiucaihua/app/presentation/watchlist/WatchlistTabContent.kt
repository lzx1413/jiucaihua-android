package com.jiucaihua.app.presentation.watchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.domain.model.WatchlistItem

@Composable
fun WatchlistTabContent(
    uiState: WatchlistUiState,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onItemLongClick: (WatchlistItem) -> Unit,
    onGroupSelected: (String?) -> Unit,
) {
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.items.isEmpty() && uiState.selectedGroup != null -> {
            // Group selected but no items in it
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "该分组暂无自选",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onGroupSelected(null) }) {
                    Text("查看全部")
                }
            }
        }
        uiState.items.isEmpty() -> {
            EmptyWatchlistState(onAddClick)
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.groups.isNotEmpty()) {
                    GroupFilterRow(
                        groups = uiState.groups,
                        selectedGroup = uiState.selectedGroup,
                        onGroupSelected = onGroupSelected,
                    )
                }
                WatchlistList(
                    items = uiState.items,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
        }
    }
}

@Composable
private fun GroupFilterRow(
    groups: List<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onGroupSelected(null) },
                label = { Text("全部") },
            )
        }
        items(groups) { group ->
            FilterChip(
                selected = selectedGroup == group,
                onClick = { onGroupSelected(group) },
                label = { Text(group) },
            )
        }
    }
}

@Composable
private fun EmptyWatchlistState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无自选",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddClick) {
                Text("添加自选")
            }
        }
    }
}

@Composable
private fun WatchlistList(
    items: List<WatchlistItem>,
    onItemClick: (String) -> Unit,
    onItemLongClick: (WatchlistItem) -> Unit,
) {
    // Group items by group field, show group headers
    val groupedItems = items.groupBy { it.group.ifEmpty { "未分组" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groupedItems.forEach { (group, groupItems) ->
            if (group != "未分组" || groupedItems.size > 1) {
                item(key = "group_$group") {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }
            items(items = groupItems, key = { it.id }) { item ->
                WatchlistItemRow(
                    item = item,
                    onClick = { onItemClick(item.code) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }
}

@Composable
private fun WatchlistItemRow(
    item: WatchlistItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val isPositive = item.changePercent >= 0
    val changeColor = if (isPositive) RiseColor else FallColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.group.isNotBlank()) {
                        Text(
                            text = item.group,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatPrice(item.currentPrice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = changeColor,
            )
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatChangePercent(item.changePercent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = changeColor,
                )
                Text(
                    text = formatChangeAmount(item.changeAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor,
                )
            }
        }
    }
}

@Composable
fun AddWatchlistDialog(
    uiState: WatchlistUiState,
    onQueryChange: (String) -> Unit,
    onResultClick: (com.jiucaihua.app.domain.model.SecuritySearchResult) -> Unit,
    onGroupChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自选") },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChange,
                    label = { Text("搜索证券名称或代码") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { }),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.addDialogGroup,
                    onValueChange = onGroupChange,
                    label = { Text("分组（可选）") },
                    placeholder = { Text("例如：银行、科技") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.groups.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(uiState.groups) { group ->
                            FilterChip(
                                selected = uiState.addDialogGroup == group,
                                onClick = { onGroupChange(group) },
                                label = { Text(group) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = uiState.isSearching) {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                AnimatedVisibility(visible = uiState.searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(uiState.searchResults) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResultClick(result) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = result.displayCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (uiState.searchQuery.isNotBlank() && !uiState.isSearching && uiState.searchResults.isEmpty()) {
                    Text(
                        text = "未找到匹配的证券",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun WatchlistGroupDialog(
    item: WatchlistItem,
    existingGroups: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var groupInput by remember { mutableStateOf(item.group) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置分组") },
        text = {
            Column {
                Text(
                    text = "${item.name}(${item.code})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = groupInput,
                    onValueChange = { groupInput = it },
                    label = { Text("分组名称") },
                    placeholder = { Text("输入分组名，留空取消分组") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingGroups.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(existingGroups) { group ->
                            FilterChip(
                                selected = groupInput == group,
                                onClick = { groupInput = group },
                                label = { Text(group) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(groupInput) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private val RiseColor = Color(0xFFE53935)
private val FallColor = Color(0xFF43A047)

private fun formatPrice(price: Double): String {
    return if (price == 0.0) "--" else String.format("%.2f", price)
}

private fun formatChangePercent(percent: Double): String {
    return if (percent == 0.0) "--" else String.format("%+.2f%%", percent)
}

private fun formatChangeAmount(amount: Double): String {
    return if (amount == 0.0) "--" else String.format("%+.2f", amount)
}
