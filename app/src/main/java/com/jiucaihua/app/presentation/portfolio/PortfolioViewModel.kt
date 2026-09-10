package com.jiucaihua.app.presentation.portfolio

import android.content.SharedPreferences
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.NewsSource
import com.jiucaihua.app.domain.model.NewsTopic
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.PortfolioPeriodReturn
import com.jiucaihua.app.domain.model.ReturnPeriod
import com.jiucaihua.app.domain.model.ReturnHistoryResult
import com.jiucaihua.app.domain.model.ReturnHistoryType
import com.jiucaihua.app.domain.model.SortOrder
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioPeriodReturnsUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioReturnHistoryUseCase
import com.jiucaihua.app.domain.usecase.GetTransactionSummaryUseCase
import com.jiucaihua.app.domain.usecase.AddTransactionUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import com.jiucaihua.app.domain.usecase.ManageHoldingUseCase
import com.jiucaihua.app.domain.usecase.RecordSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.jiucaihua.app.presentation.common.RefreshRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

data class PortfolioUiState(
    val summary: PortfolioSummary = PortfolioSummary(),
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val marketSessions: Map<MarketType, MarketSession> = emptyMap(),
    val marketNews: List<NewsFlash> = emptyList(),
    val bookmarkedNews: List<NewsFlash> = emptyList(),
    val showBookmarkedOnly: Boolean = false,
    val selectedNewsSource: NewsSource? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isNewsLoading: Boolean = false,
    val isNewsRefreshing: Boolean = false,
    val error: String? = null,
    val newsError: String? = null,
    val newsSearchQuery: String = "",
    val searchedNews: List<NewsFlash>? = null,
    val isNewsSearching: Boolean = false,
    val snapshots: List<PortfolioSnapshot> = emptyList(),
    val periodReturns: List<PortfolioPeriodReturn?> = List(ReturnPeriod.entries.size) { null },
    val returnHistory: ReturnHistoryResult? = null,
    val selectedChartRange: ChartRange = ChartRange.SEVEN_DAYS,
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val manageHoldingUseCase: ManageHoldingUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getPortfolioPeriodReturnsUseCase: GetPortfolioPeriodReturnsUseCase,
    private val getPortfolioReturnHistoryUseCase: GetPortfolioReturnHistoryUseCase,
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
    private val recordSnapshotUseCase: RecordSnapshotUseCase,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
    private val newsRepository: NewsRepository,
    private val snapshotRepository: PortfolioSnapshotRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    @param:Named("appPrefs") private val prefs: SharedPreferences,
    @param:Named("computationDispatcher") private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var holdingsVisible = false
    private var newsVisible = false
    private var newsLoaded = false
    private var quoteRevision = 0
    private val quoteRefresh = RefreshRunner(viewModelScope) { refreshPortfolio() }
    private var analyticsJob: Job? = null
    private var historyJob: Job? = null
    private var bookmarkedNewsJob: Job? = null
    private var newsRefreshJob: Job? = null
    private var storedSnapshots: List<PortfolioSnapshot> = emptyList()

    private var newsJob: Job? = null
    private var searchJob: Job? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_REFERENCE_RESET_AT) invalidateQuotes()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        loadCachedData()
        observeHoldings()
        observeSnapshots()
    }

    private fun loadCachedData() {
        viewModelScope.launch {
            try {
                val summary = withContext(computationDispatcher) { getPortfolioUseCase.getPortfolioFromCache() }
                // A slow cache fallback must not overwrite quotes that have already arrived.
                if (_uiState.value.isLoading) {
                    _uiState.update { it.copy(summary = applySorting(summary, it.sortOrder), isLoading = false) }
                    refreshAnalytics()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun observeHoldings() {
        viewModelScope.launch {
            var observed = false
            getPortfolioUseCase.observeHoldings().distinctUntilChanged().collect {
                if (observed) invalidateQuotes() else refreshQuotes()
                observed = true
            }
        }
    }

    private fun observeMarketNews() {
        newsJob?.cancel()
        newsJob = viewModelScope.launch {
            val flow = if (_uiState.value.selectedNewsSource != null) {
                newsRepository.observeNewsBySource(_uiState.value.selectedNewsSource!!)
            } else {
                newsRepository.observeAllNews()
            }
            _uiState.value = _uiState.value.copy(isNewsLoading = true, newsError = null)
            flow.collect { newsList ->
                _uiState.value = _uiState.value.copy(
                    marketNews = newsList,
                    isNewsLoading = false,
                    newsError = if (newsList.isEmpty()) context.getString(R.string.no_news) else null,
                )
            }
        }
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            snapshotRepository.observeAll().collect { snapshotList ->
                storedSnapshots = snapshotList
                refreshAnalytics()
            }
        }
    }

    private fun observeBookmarkedNews() {
        bookmarkedNewsJob?.cancel()
        bookmarkedNewsJob = viewModelScope.launch {
            newsRepository.observeBookmarkedNews().collect { bookmarkedList ->
                _uiState.value = _uiState.value.copy(bookmarkedNews = bookmarkedList)
            }
        }
    }

    fun toggleNewsBookmark(news: NewsFlash) {
        viewModelScope.launch {
            newsRepository.toggleBookmark(news.id, news.sourceType, !news.isBookmarked)
        }
    }

    fun setShowBookmarkedOnly(show: Boolean) {
        _uiState.value = _uiState.value.copy(showBookmarkedOnly = show)
    }

    fun setChartRange(range: ChartRange) {
        _uiState.value = _uiState.value.copy(selectedChartRange = range)
    }

    fun openReturnHistory(period: ReturnPeriod) {
        val type = when (period) {
            ReturnPeriod.DAY -> ReturnHistoryType.DAILY
            ReturnPeriod.MONTH -> ReturnHistoryType.MONTHLY
            ReturnPeriod.YEAR -> ReturnHistoryType.YEARLY
        }
        loadReturnHistory(type)
    }

    fun selectReturnHistoryOption(option: String) {
        _uiState.value.returnHistory?.let { loadReturnHistory(it.type, option) }
    }

    fun closeReturnHistory() {
        historyJob?.cancel()
        _uiState.value = _uiState.value.copy(returnHistory = null)
    }

    fun refreshNews() {
        if (!newsVisible || newsRefreshJob?.isActive == true) return
        newsRefreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNewsRefreshing = true)
            val topic = _uiState.value.selectedNewsSource?.let { source ->
                NewsTopic.entries.find { source in it.sources }
            }
            try {
                newsRepository.refreshNews(topic)
                newsLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(newsError = error.message) }
            } finally {
                _uiState.value = _uiState.value.copy(isNewsRefreshing = false)
            }
        }
    }

    fun setSelectedNewsSource(source: NewsSource?) {
        _uiState.value = _uiState.value.copy(selectedNewsSource = source)
        if (newsVisible) observeMarketNews()
    }

    fun setNewsVisible(visible: Boolean) {
        if (newsVisible == visible) return
        newsVisible = visible
        if (visible) {
            observeMarketNews()
            observeBookmarkedNews()
            if (!newsLoaded) refreshNews()
        } else {
            newsJob?.cancel()
            bookmarkedNewsJob?.cancel()
            newsRefreshJob?.cancel()
            searchJob?.cancel()
            _uiState.update { it.copy(isNewsSearching = false, isNewsRefreshing = false) }
        }
    }

    fun setHoldingsVisible(visible: Boolean) {
        if (holdingsVisible == visible) return
        holdingsVisible = visible
        if (visible) {
            refreshAnalytics()
            startAutoRefresh()
        } else {
            refreshJob?.cancel()
            quoteRevision++
            quoteRefresh.cancel()
            analyticsJob?.cancel()
            historyJob?.cancel()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            // Refresh once on entry even after market close, then wait between completed rounds.
            quoteRefresh.request().join()
            while (isActive) {
                val sessions = try {
                    isMarketOpenUseCase.getMarketSessions()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyMap()
                }
                _uiState.value = _uiState.value.copy(marketSessions = sessions)

                val anyTrading = sessions.values.any { it == MarketSession.TRADING }
                if (anyTrading) {
                    delay(prefs.getInt(KEY_REFRESH_INTERVAL, 10).coerceAtLeast(1) * 1000L)
                    quoteRefresh.request().join()
                } else {
                    delay(SESSION_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    fun refreshQuotes() {
        if (holdingsVisible) quoteRefresh.request()
    }

    private fun invalidateQuotes() {
        quoteRevision++
        if (holdingsVisible) quoteRefresh.request(invalidateRunning = true)
    }

    private suspend fun refreshPortfolio() {
        val revision = quoteRevision
        _uiState.update { it.copy(isRefreshing = true) }
        try {
            val summary = withContext(computationDispatcher) { getPortfolioUseCase.getPortfolioWithQuotes() }
            if (revision != quoteRevision) return
            _uiState.update {
                it.copy(summary = applySorting(summary, it.sortOrder), isLoading = false, error = null)
            }
            refreshAnalytics()
            // Reuse these quotes when recording a closing snapshot; never fetch the same portfolio twice.
            try {
                withContext(computationDispatcher) { recordSnapshotUseCase.recordSnapshot(summary) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The next foreground/background round can retry the closing snapshot.
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (revision == quoteRevision) {
                _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.quote_load_failed, error.message)) }
            }
        } finally {
            if (revision == quoteRevision) _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setSortOrder(order: SortOrder) {
        val currentSummary = _uiState.value.summary
        _uiState.value = _uiState.value.copy(
            sortOrder = order,
            summary = applySorting(currentSummary, order),
        )
    }

    fun deleteHolding(id: Long) {
        viewModelScope.launch {
            manageHoldingUseCase.deleteHolding(id)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun searchNews(query: String) {
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = _uiState.value.copy(
                newsSearchQuery = "",
                searchedNews = null,
                isNewsSearching = false,
            )
            return
        }
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(newsSearchQuery = query, isNewsSearching = true)
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val results = newsRepository.searchNews(query)
                _uiState.value = _uiState.value.copy(
                    searchedNews = results,
                    isNewsSearching = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isNewsSearching = false)
            }
        }
    }

    fun clearNewsSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            newsSearchQuery = "",
            searchedNews = null,
            isNewsSearching = false,
        )
    }

    fun setCash(value: Double) {
        val currentCash = prefs.getFloat(KEY_CASH, 0f).toDouble()
        val cashChange = value - currentCash
        if (cashChange == 0.0) {
            refreshQuotes()
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                addTransactionUseCase(
                    InvestmentTransaction(
                        type = if (cashChange > 0.0) TransactionType.CASH_IN else TransactionType.CASH_OUT,
                        tradeDate = now,
                        amount = kotlin.math.abs(cashChange),
                        note = "首页现金调整",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                invalidateQuotes()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setLossCompensation(value: Double) {
        prefs.edit().putFloat(KEY_LOSS_COMPENSATION, value.toFloat()).apply()
        invalidateQuotes()
    }

    private fun refreshAnalytics() {
        analyticsJob?.cancel()
        if (!holdingsVisible) return
        analyticsJob = viewModelScope.launch {
            val summary = _uiState.value.summary
            val stored = storedSnapshots
            try {
                val (snapshots, periodReturns) = withContext(computationDispatcher) {
                    val snapshots = stored.withCurrentQuoteSnapshot(summary)
                    snapshots to getPortfolioPeriodReturnsUseCase(
                        snapshots = snapshots,
                        currentAssetValue = summary.totalMarketValue + summary.cash,
                    )
                }
                _uiState.update { it.copy(snapshots = snapshots, periodReturns = periodReturns) }
                _uiState.value.returnHistory?.let { loadReturnHistory(it.type, it.selectedOption) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the last complete analytics result if local data is temporarily unavailable.
            }
        }
    }

    private fun loadReturnHistory(type: ReturnHistoryType, selectedOption: String? = null) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val snapshots = _uiState.value.snapshots
            try {
                val result = withContext(computationDispatcher) {
                    getPortfolioReturnHistoryUseCase(snapshots, type, selectedOption)
                }
                _uiState.update { it.copy(returnHistory = result) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }

    private fun applySorting(summary: PortfolioSummary, order: SortOrder): PortfolioSummary {
        val sortFunction: (List<Holding>) -> List<Holding> = { holdings ->
            when (order) {
                SortOrder.DEFAULT -> holdings
                SortOrder.CHANGE_PERCENT_DESC -> holdings.sortedByDescending { it.changePercent }
                SortOrder.CHANGE_PERCENT_ASC -> holdings.sortedBy { it.changePercent }
                SortOrder.EARNINGS_DESC -> holdings.sortedByDescending { it.earningsCNY }
                SortOrder.EARNINGS_ASC -> holdings.sortedBy { it.earningsCNY }
                SortOrder.MARKET_VALUE_DESC -> holdings.sortedByDescending { it.marketValueCNY }
                SortOrder.MARKET_VALUE_ASC -> holdings.sortedBy { it.marketValueCNY }
            }
        }

        val sortedCategories = summary.categorySummaries.map { category ->
            category.copy(holdings = sortFunction(category.holdings))
        }
        val sortedHoldings = sortFunction(summary.holdings)

        return summary.copy(
            holdings = sortedHoldings,
            categorySummaries = sortedCategories,
        )
    }

    /**
     * The stored snapshot is an end-of-day value. During the trading day, replace
     * today's point with the value calculated from the current per-holding quotes.
     */
    private suspend fun List<PortfolioSnapshot>.withCurrentQuoteSnapshot(summary: PortfolioSummary): List<PortfolioSnapshot> {
        if (summary.holdings.isEmpty()) return this

        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val storedToday = firstOrNull { it.date == today }
        val netExternalCashFlow = getTransactionSummaryUseCase.getNetExternalCashFlow()
        val currentSnapshot = PortfolioSnapshot(
            id = storedToday?.id ?: 0,
            date = today,
            timestamp = now,
            totalMarketValue = summary.totalMarketValue,
            totalCost = summary.totalCost,
            totalEarnings = summary.totalEarnings,
            totalEarningsPercent = summary.totalEarningsPercent,
            todayEarnings = summary.todayEarnings,
            cash = summary.cash,
            lossCompensation = summary.lossCompensation,
            categoryValues = summary.categorySummaries.associate { it.marketType.name to it.totalMarketValue },
            benchmarkPercent = storedToday?.benchmarkPercent ?: 0.0,
            netExternalCashFlow = netExternalCashFlow,
        )
        return filterNot { it.date == today }.plus(currentSnapshot).sortedBy { it.timestamp }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        refreshJob?.cancel()
        newsJob?.cancel()
        searchJob?.cancel()
    }

    companion object {
        private const val SESSION_CHECK_INTERVAL_MS = 60_000L
        private const val KEY_CASH = "cash"
        private const val KEY_LOSS_COMPENSATION = "loss_compensation"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_seconds"
        private const val KEY_REFERENCE_RESET_AT = "portfolio_reference_reset_at"
    }
}
