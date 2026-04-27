package com.jiucaihua.app.presentation.portfolio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.SortOrder
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.presentation.ai.AiChatContent
import com.jiucaihua.app.presentation.ai.AiChatViewModel
import com.jiucaihua.app.presentation.ai.AiTabIcon
import com.jiucaihua.app.presentation.market.MarketScreenContent
import com.jiucaihua.app.presentation.market.MarketViewModel
import com.jiucaihua.app.presentation.common.components.EmptyState
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.common.components.MarketStatusBadge
import com.jiucaihua.app.presentation.detail.components.StockNewsSection
import com.jiucaihua.app.presentation.portfolio.components.CategoryHoldingSection
import com.jiucaihua.app.presentation.portfolio.components.HoldingListItem
import com.jiucaihua.app.presentation.portfolio.components.PortfolioSummaryCard
import com.jiucaihua.app.presentation.portfolio.components.SortSelector

private const val HoldingsTabIndex = 0
private const val NewsTabIndex = 1
private const val AiTabIndex = 2
private const val MarketTabIndex = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onAddHolding: () -> Unit,
    onEditHolding: (Long) -> Unit,
    onHoldingClick: (String) -> Unit,
    onArticleClick: (StockArticle) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMarket: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel(),
    aiViewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    var holdingToDelete by remember { mutableStateOf<Holding?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(HoldingsTabIndex) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tabItems = listOf(
        "持仓" to Icons.Outlined.AccountBalanceWallet,
        "资讯" to Icons.AutoMirrored.Outlined.Article,
        "AI助手" to AiTabIcon,
        "大盘" to Icons.Outlined.TrendingUp,
    )

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("九财花")
                        val subtitle = when (selectedTabIndex) {
                            HoldingsTabIndex -> uiState.summary.lastUpdateTime.takeIf { it != "--" }?.let { "更新于 $it" }
                            NewsTabIndex -> "市场资讯"
                            AiTabIndex -> "智能问答"
                            else -> null
                        }
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (selectedTabIndex == HoldingsTabIndex) {
                        IconButton(onClick = onAddHolding) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加持仓",
                            )
                        }
                    }
                    MarketStatusBadge(
                        sessions = uiState.marketSessions,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "菜单",
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("预警管理") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAlerts()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabItems.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                HoldingsTabIndex -> HoldingsTabContent(
                    uiState = uiState,
                    onSortChanged = viewModel::setSortOrder,
                    onHoldingClick = onHoldingClick,
                    onHoldingLongClick = { holdingToDelete = it },
                )

                NewsTabIndex -> NewsTabContent(
                    articles = uiState.marketNews,
                    isLoading = uiState.isNewsLoading,
                    error = uiState.newsError,
                    onArticleClick = onArticleClick,
                )

                AiTabIndex -> AiChatContent(
                    uiState = aiUiState,
                    onInputChange = aiViewModel::updateInput,
                    onSend = aiViewModel::sendMessage,
                )

                MarketTabIndex -> MarketTabContent(
                    onIndexClick = onHoldingClick,
                )
            }
        }
    }

    holdingToDelete?.let { holding ->
        AlertDialog(
            onDismissRequest = { holdingToDelete = null },
            title = { Text("管理持仓") },
            text = { Text("${holding.name}(${holding.code})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHolding(holding.id)
                        holdingToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { holdingToDelete = null }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            val id = holding.id
                            holdingToDelete = null
                            onEditHolding(id)
                        }
                    ) {
                        Text("编辑")
                    }
                }
            }
        )
    }
}

@Composable
private fun HoldingsTabContent(
    uiState: PortfolioUiState,
    onSortChanged: (SortOrder) -> Unit,
    onHoldingClick: (String) -> Unit,
    onHoldingLongClick: (Holding) -> Unit,
) {
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }

        uiState.summary.holdings.isEmpty() -> {
            EmptyState()
        }

        else -> {
            HoldingsList(
                summary = uiState.summary,
                sortOrder = uiState.sortOrder,
                onSortChanged = onSortChanged,
                onHoldingClick = onHoldingClick,
                onHoldingLongClick = onHoldingLongClick,
            )
        }
    }
}

@Composable
private fun HoldingsList(
    summary: PortfolioSummary,
    sortOrder: SortOrder,
    onSortChanged: (SortOrder) -> Unit,
    onHoldingClick: (String) -> Unit,
    onHoldingLongClick: (Holding) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            PortfolioSummaryCard(summary = summary)
        }
        item {
            SortSelector(
                currentSort = sortOrder,
                onSortChanged = onSortChanged,
            )
        }
        items(
            items = summary.categorySummaries,
            key = { it.marketType.name }
        ) { categorySummary ->
            CategoryHoldingSection(
                categorySummary = categorySummary,
                onHoldingClick = onHoldingClick,
                onHoldingLongClick = onHoldingLongClick,
            )
        }
    }
}

@Composable
private fun NewsTabContent(
    articles: List<StockArticle>,
    isLoading: Boolean,
    error: String?,
    onArticleClick: (StockArticle) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            StockNewsSection(
                title = "市场资讯",
                articles = articles,
                isLoading = isLoading,
                error = error,
                onArticleClick = onArticleClick,
            )
        }
    }
}

@Composable
private fun MarketTabContent(
    onIndexClick: (String) -> Unit,
    marketViewModel: MarketViewModel = hiltViewModel(),
) {
    MarketScreenContent(
        viewModel = marketViewModel,
        onIndexClick = { index ->
            onIndexClick(index.code)
        },
    )
}
