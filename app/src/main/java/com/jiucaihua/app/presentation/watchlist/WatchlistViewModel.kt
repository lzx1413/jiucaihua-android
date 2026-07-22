package com.jiucaihua.app.presentation.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.domain.model.WatchlistItem
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val items: List<WatchlistItem> = emptyList(),
    val allItems: List<WatchlistItem> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<SecuritySearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isAddDialogVisible: Boolean = false,
    val addDialogGroup: String = "",
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
    private val securitySearchRepository: SecuritySearchRepository,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    init {
        observeWatchlist()
        observeGroups()
        startAutoRefresh()
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchlistRepository.getAllWatchlist().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        allItems = items,
                        items = filterWatchlistItems(items, state.selectedGroup),
                        isLoading = false,
                    )
                }
                refreshQuotes()
            }
        }
    }

    private fun observeGroups() {
        viewModelScope.launch {
            watchlistRepository.observeGroups().collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
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
            val currentItems = _uiState.value.items
            if (currentItems.isEmpty()) return@launch

            try {
                val updatedItems = buildList {
                    currentItems.forEach { item -> add(refreshItem(item)) }
                }
                val updatesById = updatedItems.associateBy { it.id }
                _uiState.update { state ->
                    val updatedAllItems = state.allItems.map { updatesById[it.id] ?: it }
                    state.copy(
                        allItems = updatedAllItems,
                        items = filterWatchlistItems(updatedAllItems, state.selectedGroup),
                        error = null,
                    )
                }
            } catch (_: Exception) {
                // Keep existing data on refresh failure
            }
        }
    }

    private suspend fun refreshItem(item: WatchlistItem): WatchlistItem {
        return try {
            when (item.marketType) {
                MarketType.FUND -> {
                    val quote = fundRepository.getFundQuotes(listOf(item.code)).firstOrNull() ?: return item
                    val price = quote.estimatedValue.takeIf { it > 0 } ?: quote.netAssetValue
                    if (price > 0) {
                        item.copy(currentPrice = price, changePercent = quote.dailyChangePercent)
                    } else item
                }
                else -> {
                    val quote = when (item.marketType) {
                        MarketType.A_STOCK -> stockRepository.getAStockQuotes(listOf(item.code))
                        MarketType.HK_STOCK -> stockRepository.getHKStockQuotes(listOf(item.code))
                        MarketType.US_STOCK -> stockRepository.getUSStockQuotes(listOf(item.code))
                        MarketType.GOLD -> stockRepository.getGoldQuotes(listOf(item.code))
                        MarketType.FUND -> emptyList()
                    }.firstOrNull() ?: return item
                    item.copy(
                        currentPrice = quote.price,
                        changePercent = quote.changePercent,
                        changeAmount = quote.changeAmount,
                    )
                }
            }
        } catch (_: Exception) {
            item
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(isAddDialogVisible = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun hideAddDialog() {
        searchJob?.cancel()
        _uiState.update { it.copy(isAddDialogVisible = false, searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query.trim())
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = securitySearchRepository.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        } catch (_: Exception) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun addWatchlistItem(result: SecuritySearchResult) {
        viewModelScope.launch {
            val alreadyWatched = watchlistRepository.isWatched(result.code)
            if (!alreadyWatched) {
                watchlistRepository.addWatchlistItem(result.code, result.name, result.marketType, _uiState.value.addDialogGroup)
            }
            hideAddDialog()
        }
    }

    fun setSelectedGroup(group: String?) {
        _uiState.update { state ->
            state.copy(
                selectedGroup = group,
                items = filterWatchlistItems(state.allItems, group),
            )
        }
        refreshQuotes()
    }

    fun updateGroup(item: WatchlistItem, group: String) {
        viewModelScope.launch {
            watchlistRepository.updateGroup(item.id, group)
        }
    }

    fun setAddDialogGroup(group: String) {
        _uiState.update { it.copy(addDialogGroup = group) }
    }

    fun removeWatchlistItem(id: Long) {
        viewModelScope.launch {
            watchlistRepository.removeWatchlistItem(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        refreshJob?.cancel()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 10_000L
        private const val SESSION_CHECK_INTERVAL_MS = 60_000L
    }
}

internal fun filterWatchlistItems(items: List<WatchlistItem>, group: String?): List<WatchlistItem> {
    return if (group == null) items else items.filter { it.group == group }
}

private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
