package com.jiucaihua.app.presentation.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.domain.model.WatchlistItem
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
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
                val filtered = if (_uiState.value.selectedGroup != null) {
                    items.filter { it.group == _uiState.value.selectedGroup }
                } else items
                _uiState.update { it.copy(items = filtered, isLoading = false) }
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
                val updatedItems = currentItems.map { item ->
                    val quote = fetchQuote(item.code, item.marketType)
                    if (quote != null) {
                        item.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                            changeAmount = quote.changeAmount,
                        )
                    } else item
                }
                _uiState.update { it.copy(items = updatedItems, error = null) }
            } catch (_: Exception) {
                // Keep existing data on refresh failure
            }
        }
    }

    private suspend fun fetchQuote(code: String, marketType: MarketType) = try {
        when (marketType) {
            MarketType.A_STOCK, MarketType.FUND -> stockRepository.getAStockQuotes(listOf(code))
            MarketType.HK_STOCK -> stockRepository.getHKStockQuotes(listOf(code))
            MarketType.US_STOCK -> stockRepository.getUSStockQuotes(listOf(code))
            MarketType.GOLD -> stockRepository.getGoldQuotes(listOf(code))
        }.firstOrNull()
    } catch (_: Exception) {
        null
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
        _uiState.update { it.copy(selectedGroup = group) }
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

private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
