package com.jiucaihua.app.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.model.TransactionHistoryItem
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.presentation.common.components.ErrorMessage
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.detail.components.FundNavChartView
import com.jiucaihua.app.presentation.detail.components.HoldingInfoCard
import com.jiucaihua.app.presentation.detail.components.KLineChartView
import com.jiucaihua.app.presentation.detail.components.PeriodSelector
import com.jiucaihua.app.presentation.detail.components.QuoteHeader
import com.jiucaihua.app.presentation.detail.components.StockNewsSection
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    onArticleClick: (NewsFlash) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFund = uiState.marketType == MarketType.FUND
    var showMA by rememberSaveable { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.name.ifEmpty { uiState.code }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.stockQuote == null && uiState.fundQuote == null -> {
                    ErrorMessage(message = uiState.error ?: stringResource(R.string.loading_failed))
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        val quote = uiState.stockQuote
                        val fundQuote = uiState.fundQuote

                        if (quote != null) {
                            QuoteHeader(quote = quote)
                        } else if (fundQuote != null) {
                            QuoteHeader(
                                quote = StockQuote(
                                    code = fundQuote.code,
                                    name = fundQuote.name,
                                    price = fundQuote.estimatedValue,
                                    yestClose = fundQuote.netAssetValue,
                                    open = 0.0,
                                    high = 0.0,
                                    low = 0.0,
                                    volume = 0.0,
                                    amount = 0.0,
                                    changePercent = fundQuote.dailyChangePercent,
                                    changeAmount = fundQuote.estimatedValue - fundQuote.netAssetValue,
                                    time = fundQuote.estimateTime,
                                    marketType = MarketType.FUND,
                                ),
                            )
                        }

                        if (!isFund) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PeriodSelector(
                                    selectedPeriod = uiState.selectedPeriod,
                                    onPeriodSelected = { viewModel.selectPeriod(it) },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = showMA,
                                    onClick = { showMA = !showMA },
                                    label = { Text(stringResource(R.string.moving_average)) },
                                )
                            }
                        }

                        if (uiState.isKLineLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            uiState.kLineData?.let { data ->
                                if (data.points.isNotEmpty()) {
                                    if (isFund) {
                                        FundNavChartView(kLineData = data)
                                    } else {
                                        KLineChartView(kLineData = data, showMA = showMA)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 60.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (isFund) {
                                                stringResource(R.string.no_nav_data)
                                            } else {
                                                stringResource(R.string.no_kline_data)
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        uiState.holding?.let { holding ->
                            if (holding.currentPrice > 0) {
                                HoldingInfoCard(holding = holding)
                            }
                            TransactionHistorySection(
                                items = uiState.transactionHistory,
                                marketType = holding.marketType,
                            )
                        }

                        if (uiState.newsLoaded) {
                            StockNewsSection(
                                articles = uiState.newsArticles,
                                isLoading = uiState.isNewsLoading,
                                error = uiState.newsError,
                                onArticleClick = onArticleClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistorySection(
    items: List<TransactionHistoryItem>,
    marketType: MarketType,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_transaction_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) R.string.action_collapse else R.string.action_expand,
                        ),
                    )
                }
            }

            if (!expanded) return@Column

            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_transaction_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            items.forEachIndexed { index, item ->
                TransactionHistoryRow(
                    item = item,
                    marketType = marketType,
                    tradeDate = dateFormatter.format(Date(item.transaction.tradeDate)),
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryRow(
    item: TransactionHistoryItem,
    marketType: MarketType,
    tradeDate: String,
) {
    val transaction = item.transaction
    val valueColor = when {
        item.realizedPnlCny > 0 -> RiseRed
        item.realizedPnlCny < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = transactionTypeLabel(transaction.type),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = tradeDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (transaction.type) {
            TransactionType.BUY -> {
                Text(
                    text = "${formatQuantity(transaction.quantity, marketType)} · ${formatPrice(transaction.price)} · " +
                        "${stringResource(R.string.transaction_cost_basis)} ${formatMoney(item.costBasisCny)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TransactionType.SELL -> {
                Text(
                    text = "${formatQuantity(transaction.quantity, marketType)} · ${formatPrice(transaction.price)} · " +
                        "${stringResource(R.string.transaction_proceeds)} ${formatMoney(item.proceedsCny)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${stringResource(R.string.transaction_fifo_realized_pnl)} ${formatSignedMoney(item.realizedPnlCny)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                )
                if (item.unmatchedQuantity > 0.0) {
                    Text(
                        text = stringResource(
                            R.string.transaction_unmatched_quantity,
                            formatPlainQuantity(item.unmatchedQuantity),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            TransactionType.DIVIDEND -> {
                Text(
                    text = "${stringResource(R.string.transaction_dividend_income)} ${formatMoney(item.dividendIncomeCny)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RiseRed,
                )
            }
            else -> {
                Text(
                    text = formatMoney(transaction.amountCny),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun transactionTypeLabel(type: TransactionType): String {
    return when (type) {
        TransactionType.BUY -> stringResource(R.string.trade_buy)
        TransactionType.SELL -> stringResource(R.string.trade_sell)
        TransactionType.DIVIDEND -> stringResource(R.string.trade_dividend)
        TransactionType.FEE -> "Fee"
        TransactionType.TAX -> "Tax"
        TransactionType.SPLIT -> "Split"
        TransactionType.CASH_IN -> "Cash in"
        TransactionType.CASH_OUT -> "Cash out"
    }
}

private fun formatQuantity(quantity: Double, marketType: MarketType): String {
    return when (marketType) {
        MarketType.FUND -> "%.2f份".format(quantity)
        MarketType.GOLD -> "%.2f克".format(quantity)
        else -> "%.0f股".format(quantity)
    }
}

private fun formatPlainQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        "%.0f".format(quantity)
    } else {
        "%.4f".format(quantity).trimEnd('0').trimEnd('.')
    }
}

private fun formatPrice(price: Double): String = "¥%,.4f".format(price)

private fun formatMoney(value: Double): String = "¥%,.2f".format(value)

private fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${formatMoney(value)}"
}
