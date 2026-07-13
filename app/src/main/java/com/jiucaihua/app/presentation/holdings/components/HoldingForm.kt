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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.MarketIndexCodes
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.presentation.holdings.HoldingTradeAction
import com.jiucaihua.app.presentation.holdings.AddEditHoldingUiState
import com.jiucaihua.app.presentation.i18n.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingForm(
    state: AddEditHoldingUiState,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onMarketTypeChange: (MarketType) -> Unit,
    onCostPriceChange: (String) -> Unit,
    onHoldingSharesChange: (String) -> Unit,
    onTradeActionChange: (HoldingTradeAction) -> Unit,
    onSelectResult: (SecuritySearchResult) -> Unit,
    onDismissSearch: () -> Unit,
    onGoldPresetSelected: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.market_type),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketType.entries.forEach { type ->
                FilterChip(
                    selected = state.marketType == type,
                    onClick = { onMarketTypeChange(type) },
                    label = { Text(type.localizedLabel()) },
                    enabled = !state.isEditing,
                )
            }
        }

        val isDropdownVisible = state.searchExpanded && state.searchResults.isNotEmpty()

        if (state.marketType == MarketType.GOLD) {
            var goldExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = goldExpanded,
                onExpandedChange = { if (!state.isEditing) goldExpanded = it },
            ) {
                OutlinedTextField(
                    value = if (state.code.isNotBlank()) "${state.name} (${state.code})" else "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.gold_product)) },
                    placeholder = { Text(stringResource(R.string.select_gold_product)) },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goldExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !state.isEditing),
                    enabled = !state.isEditing,
                )
                ExposedDropdownMenu(
                    expanded = goldExpanded,
                    onDismissRequest = { goldExpanded = false },
                ) {
                    MarketIndexCodes.GOLD_HOLDING_PRESETS.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(name)
                                    Text(
                                        code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onGoldPresetSelected(code, name)
                                goldExpanded = false
                            },
                        )
                    }
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = isDropdownVisible,
                onExpandedChange = { },
            ) {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    label = { Text(stringResource(R.string.security_code)) },
                    placeholder = {
                        Text(
                            when (state.marketType) {
                                MarketType.A_STOCK -> stringResource(R.string.search_a_stock_hint)
                                MarketType.HK_STOCK -> stringResource(R.string.search_hk_stock_hint)
                                MarketType.US_STOCK -> stringResource(R.string.search_us_stock_hint)
                                MarketType.FUND -> stringResource(R.string.search_fund_hint)
                                MarketType.GOLD -> ""
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = !state.isEditing),
                    enabled = !state.isEditing,
                    supportingText = if (state.isSearching) {
                        { Text(stringResource(R.string.searching)) }
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
                                        "${result.displayCode} · ${result.marketType.localizedLabel()}",
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
        }

        if (state.marketType != MarketType.GOLD) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.security_name)) },
                placeholder = { Text(stringResource(R.string.security_name_auto_fill)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isEditing,
            )
        }

        if (state.isEditing) {
            val currentSharesLabel = when (state.marketType) {
                MarketType.FUND -> stringResource(R.string.current_fund_shares)
                MarketType.GOLD -> stringResource(R.string.current_gold_quantity)
                else -> stringResource(R.string.current_stock_shares)
            }
            Text(
                text = stringResource(
                    R.string.current_position_format,
                    currentSharesLabel,
                    state.originalHoldingShares,
                    state.originalCostPrice,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HoldingTradeAction.entries.forEach { action ->
                    FilterChip(
                        selected = state.tradeAction == action,
                        onClick = { onTradeActionChange(action) },
                        label = { Text(action.localizedLabel()) },
                    )
                }
            }
        }

        val costLabel = when {
            state.isEditing && state.tradeAction == HoldingTradeAction.DIVIDEND -> stringResource(R.string.dividend_amount)
            state.isEditing && state.marketType == MarketType.GOLD -> stringResource(R.string.trade_price_gold)
            state.isEditing -> stringResource(R.string.trade_price)
            state.marketType == MarketType.GOLD -> stringResource(R.string.cost_price_gold)
            else -> stringResource(R.string.cost_price)
        }
        OutlinedTextField(
            value = state.costPrice,
            onValueChange = onCostPriceChange,
            label = { Text(costLabel) },
            placeholder = { Text("0.00") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        if (!state.isEditing || state.tradeAction != HoldingTradeAction.DIVIDEND) {
            val sharesLabel = when (state.marketType) {
                MarketType.FUND -> if (state.isEditing) {
                    stringResource(R.string.trade_fund_shares)
                } else {
                    stringResource(R.string.holding_fund_shares)
                }
                MarketType.GOLD -> if (state.isEditing) {
                    stringResource(R.string.trade_gold_quantity)
                } else {
                    stringResource(R.string.holding_gold_quantity)
                }
                else -> if (state.isEditing) {
                    stringResource(R.string.trade_stock_quantity)
                } else {
                    stringResource(R.string.holding_stock_quantity)
                }
            }
            OutlinedTextField(
                value = state.holdingShares,
                onValueChange = onHoldingSharesChange,
                label = { Text(sharesLabel) },
                placeholder = { Text("0") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val amount = if (state.isEditing && state.tradeAction == HoldingTradeAction.DIVIDEND) {
            state.costPrice.toDoubleOrNull() ?: 0.0
        } else {
            state.holdingAmount
        }
        if (amount > 0) {
            Text(
                text = if (state.isEditing && state.tradeAction == HoldingTradeAction.DIVIDEND) {
                    stringResource(R.string.dividend_cash_in)
                } else if (state.isEditing) {
                    stringResource(R.string.trade_amount_auto)
                } else {
                    stringResource(R.string.invest_amount_auto)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "¥%,.2f".format(amount),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (
            state.isEditing &&
            state.tradeAction == HoldingTradeAction.SELL &&
            (state.holdingShares.toDoubleOrNull() ?: 0.0) > state.originalHoldingShares
        ) {
            Text(
                text = stringResource(R.string.sell_exceeds_holding),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
