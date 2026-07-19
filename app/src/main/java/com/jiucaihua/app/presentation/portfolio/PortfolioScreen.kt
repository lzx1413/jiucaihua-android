package com.jiucaihua.app.presentation.portfolio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.NewsSource
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.model.PortfolioPeriodReturn
import com.jiucaihua.app.domain.model.ReturnPeriod
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.SortOrder
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import com.jiucaihua.app.domain.model.WatchlistItem
import com.jiucaihua.app.presentation.market.MarketScreenContent
import com.jiucaihua.app.presentation.market.MarketViewModel
import com.jiucaihua.app.presentation.common.components.EmptyState
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.common.components.MarketStatusBadge
import com.jiucaihua.app.presentation.i18n.localizedLabel
import com.jiucaihua.app.presentation.portfolio.components.CategoryHoldingSection
import com.jiucaihua.app.presentation.portfolio.components.EarningsChartView
import com.jiucaihua.app.presentation.portfolio.components.HoldingListItem
import com.jiucaihua.app.presentation.portfolio.components.PortfolioSummaryCard
import com.jiucaihua.app.presentation.portfolio.components.PeriodReturnsGrid
import com.jiucaihua.app.presentation.portfolio.components.ReturnHistoryDialog
import com.jiucaihua.app.presentation.portfolio.components.SortSelector
import com.jiucaihua.app.presentation.watchlist.AddWatchlistDialog
import com.jiucaihua.app.presentation.watchlist.WatchlistGroupDialog
import com.jiucaihua.app.presentation.watchlist.WatchlistTabContent
import com.jiucaihua.app.presentation.watchlist.WatchlistUiState
import com.jiucaihua.app.presentation.watchlist.WatchlistViewModel

private const val HoldingsTabIndex = 0
private const val NewsTabIndex = 1
private const val WatchlistTabIndex = 2
private const val MarketTabIndex = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onAddHolding: () -> Unit,
    onEditHolding: (Long) -> Unit,
    onHoldingClick: (String) -> Unit,
    onArticleClick: (NewsFlash) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMarket: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel(),
    watchlistViewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchlistUiState by watchlistViewModel.uiState.collectAsStateWithLifecycle()
    var holdingToDelete by remember { mutableStateOf<Holding?>(null) }
    var watchlistToDelete by remember { mutableStateOf<WatchlistItem?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTabIndex by rememberSaveable { mutableStateOf(HoldingsTabIndex) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tabItems = listOf(
        stringResource(R.string.tab_holdings) to Icons.Outlined.AccountBalanceWallet,
        stringResource(R.string.tab_news) to Icons.AutoMirrored.Outlined.Article,
        stringResource(R.string.tab_watchlist) to Icons.Outlined.StarOutline,
        stringResource(R.string.tab_market) to Icons.Outlined.TrendingUp,
    )

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.alerts)) },
                    label = { Text(stringResource(R.string.alerts)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAlerts()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings)) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.action_menu),
                        )
                    }
                },
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        val subtitle = when (selectedTabIndex) {
                            HoldingsTabIndex -> uiState.summary.lastUpdateTime.takeIf { it != "--" }?.let {
                                stringResource(R.string.updated_at, it)
                            }
                            NewsTabIndex -> stringResource(R.string.market_news)
                            WatchlistTabIndex -> stringResource(R.string.watchlist_quotes)
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
                                contentDescription = stringResource(R.string.add_holding),
                            )
                        }
                    }
                    if (selectedTabIndex == WatchlistTabIndex) {
                        IconButton(onClick = watchlistViewModel::showAddDialog) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_watchlist),
                            )
                        }
                    }
                    MarketStatusBadge(
                        sessions = uiState.marketSessions,
                        modifier = Modifier.padding(end = 4.dp),
                    )
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
    ) { paddingValues ->        Box(
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
                    onSetCash = viewModel::setCash,
                    onSetLossCompensation = viewModel::setLossCompensation,
                    onChartRangeChanged = viewModel::setChartRange,
                    onPeriodReturnClick = viewModel::openReturnHistory,
                    onReturnHistoryOptionSelect = viewModel::selectReturnHistoryOption,
                    onReturnHistoryDismiss = viewModel::closeReturnHistory,
                )

                NewsTabIndex -> NewsTabContent(
                    articles = uiState.marketNews,
                    bookmarkedNews = uiState.bookmarkedNews,
                    showBookmarkedOnly = uiState.showBookmarkedOnly,
                    selectedSource = uiState.selectedNewsSource,
                    onSourceSelected = viewModel::setSelectedNewsSource,
                    isLoading = uiState.isNewsLoading,
                    isRefreshing = uiState.isNewsRefreshing,
                    error = uiState.newsError,
                    onRefresh = viewModel::refreshNews,
                    onArticleClick = onArticleClick,
                    searchQuery = uiState.newsSearchQuery,
                    searchedNews = uiState.searchedNews,
                    isNewsSearching = uiState.isNewsSearching,
                    onSearchQueryChange = viewModel::searchNews,
                    onClearSearch = viewModel::clearNewsSearch,
                    onBookmarkToggle = viewModel::toggleNewsBookmark,
                    onShowBookmarkedOnly = viewModel::setShowBookmarkedOnly,
                )

                WatchlistTabIndex -> WatchlistTabContent(
                    uiState = watchlistUiState,
                    onAddClick = watchlistViewModel::showAddDialog,
                    onItemClick = onHoldingClick,
                    onItemLongClick = { watchlistToDelete = it },
                    onGroupSelected = watchlistViewModel::setSelectedGroup,
                )

                MarketTabIndex -> MarketTabContent(
                    onIndexClick = onHoldingClick,
                )
            }
        }
    }
    }

    holdingToDelete?.let { holding ->
        AlertDialog(
            onDismissRequest = { holdingToDelete = null },
            title = { Text(stringResource(R.string.manage_holding)) },
            text = { Text("${holding.name}(${holding.code})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHolding(holding.id)
                        holdingToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { holdingToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            val id = holding.id
                            holdingToDelete = null
                            onEditHolding(id)
                        }
                    ) {
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        )
    }

    watchlistToDelete?.let { item ->
        var showGroupDialog by remember { mutableStateOf(false) }

        if (showGroupDialog) {
            WatchlistGroupDialog(
                item = item,
                existingGroups = watchlistUiState.groups,
                onConfirm = { group ->
                    watchlistViewModel.updateGroup(item, group)
                    showGroupDialog = false
                    watchlistToDelete = null
                },
                onDismiss = {
                    showGroupDialog = false
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { watchlistToDelete = null },
                title = { Text(stringResource(R.string.manage_watchlist)) },
                text = { Text("${item.name}(${item.code})") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            watchlistViewModel.removeWatchlistItem(item.id)
                            watchlistToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { watchlistToDelete = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                showGroupDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.set_group))
                        }
                    }
                },
            )
        }
    }

    if (watchlistUiState.isAddDialogVisible) {
        AddWatchlistDialog(
            uiState = watchlistUiState,
            onQueryChange = watchlistViewModel::onSearchQueryChange,
            onResultClick = watchlistViewModel::addWatchlistItem,
            onGroupChange = watchlistViewModel::setAddDialogGroup,
            onDismiss = watchlistViewModel::hideAddDialog,
        )
    }
}

@Composable
private fun HoldingsTabContent(
    uiState: PortfolioUiState,
    onSortChanged: (SortOrder) -> Unit,
    onHoldingClick: (String) -> Unit,
    onHoldingLongClick: (Holding) -> Unit,
    onSetCash: (Double) -> Unit,
    onSetLossCompensation: (Double) -> Unit,
    onChartRangeChanged: (ChartRange) -> Unit,
    onPeriodReturnClick: (ReturnPeriod) -> Unit,
    onReturnHistoryOptionSelect: (String) -> Unit,
    onReturnHistoryDismiss: () -> Unit,
) {
    uiState.returnHistory?.let { history ->
        ReturnHistoryDialog(
            result = history,
            onOptionSelected = onReturnHistoryOptionSelect,
            onDismiss = onReturnHistoryDismiss,
        )
    }
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
                periodReturns = uiState.periodReturns,
                sortOrder = uiState.sortOrder,
                snapshots = uiState.snapshots,
                selectedChartRange = uiState.selectedChartRange,
                onSortChanged = onSortChanged,
                onHoldingClick = onHoldingClick,
                onHoldingLongClick = onHoldingLongClick,
                onSetCash = onSetCash,
                onSetLossCompensation = onSetLossCompensation,
                onChartRangeChanged = onChartRangeChanged,
                onPeriodReturnClick = onPeriodReturnClick,
            )
        }
    }
}

@Composable
private fun HoldingsList(
    summary: PortfolioSummary,
    periodReturns: List<PortfolioPeriodReturn?>,
    sortOrder: SortOrder,
    snapshots: List<PortfolioSnapshot>,
    selectedChartRange: ChartRange,
    onSortChanged: (SortOrder) -> Unit,
    onHoldingClick: (String) -> Unit,
    onHoldingLongClick: (Holding) -> Unit,
    onSetCash: (Double) -> Unit,
    onSetLossCompensation: (Double) -> Unit,
    onChartRangeChanged: (ChartRange) -> Unit,
    onPeriodReturnClick: (ReturnPeriod) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            PortfolioSummaryCard(
                summary = summary,
                onSetCash = onSetCash,
                onSetLossCompensation = onSetLossCompensation,
            )
        }
        if (snapshots.isNotEmpty()) {
            item {
                EarningsChartSection(
                    snapshots = snapshots,
                    periodReturns = periodReturns,
                    selectedChartRange = selectedChartRange,
                    onChartRangeChanged = onChartRangeChanged,
                    onPeriodReturnClick = onPeriodReturnClick,
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsTabContent(
    articles: List<NewsFlash>,
    bookmarkedNews: List<NewsFlash>,
    showBookmarkedOnly: Boolean,
    selectedSource: NewsSource?,
    onSourceSelected: (NewsSource?) -> Unit,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onArticleClick: (NewsFlash) -> Unit,
    searchQuery: String,
    searchedNews: List<NewsFlash>?,
    isNewsSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onBookmarkToggle: (NewsFlash) -> Unit,
    onShowBookmarkedOnly: (Boolean) -> Unit,
) {
    val displayedArticles = searchedNews ?: if (showBookmarkedOnly) bookmarkedNews else articles
    val filtered = if (selectedSource == null) displayedArticles
        else displayedArticles.filter { it.sourceType == selectedSource }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.search_news)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            NewsSourceFilterRow(
                selectedSource = selectedSource,
                onSourceSelected = onSourceSelected,
            )
            // Bookmark filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = showBookmarkedOnly,
                    onClick = { onShowBookmarkedOnly(!showBookmarkedOnly) },
                    label = { Text(stringResource(R.string.bookmarked_only)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showBookmarkedOnly) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.action_bookmark),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
            when {
                isNewsSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isLoading && searchedNews == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null && searchedNews == null -> {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                filtered.isEmpty() -> {
                    Text(
                        text = if (searchedNews != null) {
                            stringResource(R.string.news_not_found)
                        } else if (showBookmarkedOnly) {
                            stringResource(R.string.no_bookmarked_news)
                        } else {
                            stringResource(R.string.no_news)
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = filtered,
                            key = { "${it.sourceType.name}-${it.id}" },
                        ) { article ->
                            NewsListItem(
                                article = article,
                                onClick = { onArticleClick(article) },
                                onBookmarkToggle = { onBookmarkToggle(article) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsListItem(
    article: NewsFlash,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onBookmarkToggle,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (article.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (article.isBookmarked) {
                        stringResource(R.string.action_unbookmark)
                    } else {
                        stringResource(R.string.action_bookmark)
                    },
                    tint = if (article.isBookmarked) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NewsSourceDot(sourceType = article.sourceType)
            Text(
                text = article.source,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (article.impact.isNotBlank()) {
                Text(
                    text = article.impact,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (article.impact == "利好") RiseRed else FallGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (article.time.isNotBlank()) {
                Text(
                    text = article.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = article.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewsSourceDot(sourceType: NewsSource) {
    val color = when (sourceType) {
        NewsSource.STCN -> Color(0xFFE53935)
        NewsSource.XUANGUBAO -> Color(0xFF1565C0)
        NewsSource.CLS -> Color(0xFFFF6F00)
        NewsSource.WALLSTREETCN -> Color(0xFF00897B)
        NewsSource.JIN10 -> Color(0xFF6A1B9A)
        NewsSource.EASTMONEY -> Color(0xFF2E7D32)
        NewsSource.JIUYAN -> Color(0xFF5D4037)
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun NewsSourceFilterRow(
    selectedSource: NewsSource?,
    onSourceSelected: (NewsSource?) -> Unit,
) {
    val sources = listOf<NewsSource?>(null) + NewsSource.entries.filter { it != NewsSource.JIUYAN }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(sources) { source ->
            FilterChip(
                selected = selectedSource == source,
                onClick = { onSourceSelected(source) },
                label = { Text(source?.displayName ?: stringResource(R.string.all)) },
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

@Composable
private fun EarningsChartSection(
    snapshots: List<PortfolioSnapshot>,
    periodReturns: List<PortfolioPeriodReturn?>,
    selectedChartRange: ChartRange,
    onChartRangeChanged: (ChartRange) -> Unit,
    onPeriodReturnClick: (ReturnPeriod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleSnapshots = remember(snapshots, selectedChartRange) {
        snapshots.filterByRange(selectedChartRange)
    }

    val latestSnapshot = visibleSnapshots.lastOrNull()
    val benchmarkPercent = latestSnapshot?.let { it.benchmarkPercent - (visibleSnapshots.firstOrNull()?.benchmarkPercent ?: 0.0) } ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.earnings_trend),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(12.dp))
                if (benchmarkPercent != 0.0) {
                    Text(
                        text = stringResource(R.string.csi300_percent, String.format("%.2f%%", benchmarkPercent)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.action_collapse)
                    } else {
                        stringResource(R.string.action_expand)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded content - chart
            AnimatedVisibility(visible = expanded) {
                Column {
                    PeriodReturnsGrid(
                        periodReturns = periodReturns,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onPeriodClick = onPeriodReturnClick,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChartRange.entries.forEach { range ->
                            FilterChip(
                                selected = selectedChartRange == range,
                                onClick = { onChartRangeChanged(range) },
                                label = { Text(range.localizedLabel()) },
                            )
                        }
                    }
                    EarningsChartView(
                        snapshots = visibleSnapshots,
                        selectedRange = selectedChartRange,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun List<PortfolioSnapshot>.filterByRange(range: ChartRange): List<PortfolioSnapshot> {
    if (range == ChartRange.ALL || isEmpty()) return this

    val latestTimestamp = maxOf { it.timestamp }
    val rangeDuration = when (range) {
        ChartRange.SEVEN_DAYS -> 7L * 24 * 60 * 60 * 1000
        ChartRange.THIRTY_DAYS -> 30L * 24 * 60 * 60 * 1000
        ChartRange.NINETY_DAYS -> 90L * 24 * 60 * 60 * 1000
        ChartRange.ALL -> 0L
    }
    val fromTimestamp = latestTimestamp - rangeDuration

    return filter { it.timestamp >= fromTimestamp }
}
