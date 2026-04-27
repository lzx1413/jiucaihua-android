package com.jiucaihua.app.presentation.holdings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.presentation.common.components.ErrorMessage
import com.jiucaihua.app.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SecuritySearchSheet(
    query: String,
    selectedMarketType: MarketType?,
    results: List<SecuritySearchResult>,
    isSearching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onMarketTypeChange: (MarketType?) -> Unit,
    onRetry: () -> Unit,
    onSelect: (SecuritySearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "搜索证券",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("代码或名称") },
                placeholder = { Text("如 600519 / 腾讯控股 / 易方达") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedMarketType == null,
                    onClick = { onMarketTypeChange(null) },
                    label = { Text("全部") },
                )
                MarketType.entries.forEach { marketType ->
                    FilterChip(
                        selected = selectedMarketType == marketType,
                        onClick = { onMarketTypeChange(marketType) },
                        label = { Text(marketType.label) },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 420.dp),
            ) {
                when {
                    isSearching -> {
                        LoadingIndicator(modifier = Modifier.fillMaxSize())
                    }

                    error != null -> {
                        ErrorMessage(
                            message = error,
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    query.isBlank() -> {
                        SearchSheetHint(
                            message = "输入代码或名称开始搜索",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    results.isEmpty() -> {
                        SearchSheetHint(
                            message = "未找到匹配证券",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = results,
                                key = { it.code },
                            ) { result ->
                                ListItem(
                                    headlineContent = {
                                        Text(result.name)
                                    },
                                    supportingContent = {
                                        Text(result.displayCode)
                                    },
                                    trailingContent = {
                                        Text(
                                            text = result.marketType.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(result) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSheetHint(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
