package com.jiucaihua.app.presentation.holdings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.presentation.holdings.AddEditHoldingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingForm(
    state: AddEditHoldingUiState,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onMarketTypeChange: (MarketType) -> Unit,
    onCostPriceChange: (String) -> Unit,
    onHoldingSharesChange: (String) -> Unit,
    onSelectResult: (SecuritySearchResult) -> Unit,
    onDismissSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "市场类型",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketType.entries.forEach { type ->
                FilterChip(
                    selected = state.marketType == type,
                    onClick = { onMarketTypeChange(type) },
                    label = { Text(type.label) },
                    enabled = !state.isEditing,
                )
            }
        }

        val isDropdownVisible = state.searchExpanded && state.searchResults.isNotEmpty()

        ExposedDropdownMenuBox(
            expanded = isDropdownVisible,
            onExpandedChange = { },
        ) {
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = { Text("证券代码") },
                placeholder = {
                    Text(
                        when (state.marketType) {
                            MarketType.A_STOCK -> "输入代码或名称搜索，如 600519"
                            MarketType.HK_STOCK -> "输入代码或名称搜索，如 hk00700"
                            MarketType.FUND -> "输入代码或名称搜索，如 110011"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = !state.isEditing),
                enabled = !state.isEditing,
                supportingText = if (state.isSearching) {
                    { Text("搜索中...") }
                } else if (state.searchError != null) {
                    { Text(state.searchError) }
                } else null,
            )
            ExposedDropdownMenu(
                expanded = isDropdownVisible,
                onDismissRequest = onDismissSearch,
            ) {
                state.searchResults.forEach { result ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(result.name)
                                Text(
                                    "${result.displayCode} · ${result.marketType.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = { onSelectResult(result) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("证券名称") },
            placeholder = { Text("选择证券后自动填充") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditing,
        )

        OutlinedTextField(
            value = state.costPrice,
            onValueChange = onCostPriceChange,
            label = { Text("成本价") },
            placeholder = { Text("0.00") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        val sharesLabel = if (state.marketType == MarketType.FUND) "持仓份额" else "持仓数量（股）"
        OutlinedTextField(
            value = state.holdingShares,
            onValueChange = onHoldingSharesChange,
            label = { Text(sharesLabel) },
            placeholder = { Text("0") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val amount = state.holdingAmount
        if (amount > 0) {
            Text(
                text = "投资金额（自动计算）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "¥%,.2f".format(amount),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
