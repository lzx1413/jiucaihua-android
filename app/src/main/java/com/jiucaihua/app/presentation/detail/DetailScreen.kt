package com.jiucaihua.app.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.presentation.common.components.ErrorMessage
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.detail.components.FundNavChartView
import com.jiucaihua.app.presentation.detail.components.HoldingInfoCard
import com.jiucaihua.app.presentation.detail.components.KLineChartView
import com.jiucaihua.app.presentation.detail.components.PeriodSelector
import com.jiucaihua.app.presentation.detail.components.QuoteHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFund = uiState.marketType == MarketType.FUND

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
                            contentDescription = "返回",
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
                    ErrorMessage(message = uiState.error ?: "加载失败")
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
                            PeriodSelector(
                                selectedPeriod = uiState.selectedPeriod,
                                onPeriodSelected = { viewModel.selectPeriod(it) },
                            )
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
                                        KLineChartView(kLineData = data)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 60.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (isFund) "暂无净值数据" else "暂无K线数据",
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
                    }
                }
            }
        }
    }
}
