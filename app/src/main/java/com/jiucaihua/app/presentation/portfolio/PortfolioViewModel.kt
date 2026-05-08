package com.jiucaihua.app.presentation.portfolio

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NewsSource
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.SortOrder
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import com.jiucaihua.app.domain.usecase.ManageHoldingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class PortfolioUiState(
    val summary: PortfolioSummary = PortfolioSummary(),
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val marketSessions: Map<MarketType, MarketSession> = emptyMap(),
    val marketNews: List<StockArticle> = emptyList(),
    val selectedNewsSource: NewsSource? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isNewsLoading: Boolean = false,
    val error: String? = null,
    val newsError: String? = null,
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val manageHoldingUseCase: ManageHoldingUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
    private val newsRepository: NewsRepository,
    @Named("appPrefs") private val prefs: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadCachedData()
        observeHoldings()
        loadMarketNews()
        startAutoRefresh()
    }

    private fun loadCachedData() {
        viewModelScope.launch {
            try {
                val summary = getPortfolioUseCase.getPortfolioFromCache()
                _uiState.value = _uiState.value.copy(
                    summary = applySorting(summary, _uiState.value.sortOrder),
                    isLoading = false,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun observeHoldings() {
        viewModelScope.launch {
            getPortfolioUseCase.observeHoldings().collect {
                refreshQuotes()
            }
        }
    }

    private fun loadMarketNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNewsLoading = true, newsError = null)
            try {
                val articles = newsRepository.getMarketNews(limit = 30).map {
                    StockArticle(
                        title = it.title,
                        summary = it.summary,
                        content = it.content,
                        source = it.source,
                        time = it.time,
                        sourceType = it.sourceType,
                        impact = it.impact,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    marketNews = articles,
                    isNewsLoading = false,
                    newsError = null,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    marketNews = emptyList(),
                    isNewsLoading = false,
                    newsError = "暂无资讯",
                )
            }
        }
    }

    fun setSelectedNewsSource(source: NewsSource?) {
        _uiState.value = _uiState.value.copy(selectedNewsSource = source)
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                val sessions = try {
                    isMarketOpenUseCase.getMarketSessions()
                } catch (_: Exception) {
                    emptyMap()
                }
                _uiState.value = _uiState.value.copy(marketSessions = sessions)

                val anyTrading = sessions.values.any { it == MarketSession.TRADING }
                if (anyTrading) {
                    refreshQuotes()
                    delay(REFRESH_INTERVAL_MS)
                } else {
                    delay(SESSION_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    fun refreshQuotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                val summary = getPortfolioUseCase.getPortfolioWithQuotes()
                _uiState.value = _uiState.value.copy(
                    summary = applySorting(summary, _uiState.value.sortOrder),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "行情获取失败: ${e.message}",
                )
            }
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

    fun setCash(value: Double) {
        prefs.edit().putFloat(KEY_CASH, value.toFloat()).apply()
        refreshQuotes()
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

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 10_000L
        private const val SESSION_CHECK_INTERVAL_MS = 60_000L
        private const val KEY_CASH = "cash"
    }
}
