package com.jiucaihua.app.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.jiucaihua.app.presentation.common.components.ErrorMessage
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.detail.components.FundNavChartView
import com.jiucaihua.app.presentation.detail.components.HoldingInfoCard
import com.jiucaihua.app.presentation.detail.components.KLineChartView
import com.jiucaihua.app.presentation.detail.components.PeriodSelector
import com.jiucaihua.app.presentation.detail.components.QuoteHeader
import com.jiucaihua.app.presentation.detail.components.StockNewsSection

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
