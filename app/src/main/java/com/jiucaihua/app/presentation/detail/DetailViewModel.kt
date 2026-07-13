package com.jiucaihua.app.presentation.detail

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.model.TransactionHistoryItem
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.usecase.GetHoldingTransactionHistoryUseCase
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class DetailUiState(
    val code: String = "",
    val name: String = "",
    val marketType: MarketType = MarketType.A_STOCK,
    val stockQuote: StockQuote? = null,
    val fundQuote: FundQuote? = null,
    val holding: Holding? = null,
    val kLineData: KLineData? = null,
    val selectedPeriod: KLinePeriod = KLinePeriod.DAILY,
    val isLoading: Boolean = true,
    val isKLineLoading: Boolean = false,
    val error: String? = null,
    val newsArticles: List<NewsFlash> = emptyList(),
    val isNewsLoading: Boolean = false,
    val newsError: String? = null,
    val newsLoaded: Boolean = false,
    val transactionHistory: List<TransactionHistoryItem> = emptyList(),
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
    private val holdingRepository: HoldingRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val newsRepository: NewsRepository,
    private val getHoldingTransactionHistoryUseCase: GetHoldingTransactionHistoryUseCase,
    private val getKLineDataUseCase: GetKLineDataUseCase,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
    @param:Named("appPrefs") private val prefs: SharedPreferences,
) : ViewModel() {

    private val code: String = savedStateHandle.get<String>("code") ?: ""

    private val _uiState = MutableStateFlow(DetailUiState(code = code))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var kLineJob: Job? = null

    init {
        val marketType = MarketType.fromCode(code)
        _uiState.value = _uiState.value.copy(marketType = marketType)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                loadHolding()
                loadTransactionHistory()
                loadQuote()
                loadKLine(_uiState.value.selectedPeriod)
                loadNews()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.data_load_failed, e.message),
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(prefs.getInt(KEY_REFRESH_INTERVAL, 10) * 1000L)
                val trading = try {
                    isMarketOpenUseCase.isMarketTrading(_uiState.value.marketType)
                } catch (_: Exception) {
                    false
                }
                if (trading) {
                    try {
                        loadQuote()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private suspend fun loadHolding() {
        val holding = holdingRepository.getHoldingByCode(code) ?: return
        val marketType = holding.marketType
        val exchangeRate = if (marketType == MarketType.HK_STOCK) {
            try { exchangeRateRepository.getHkdToCnyRate() } catch (_: Exception) { DEFAULT_HKD_RATE }
        } else 1.0
        _uiState.value = _uiState.value.copy(
            marketType = marketType,
            holding = holding.copy(exchangeRate = exchangeRate),
            name = holding.name,
        )
    }

    private suspend fun loadTransactionHistory() {
        val marketType = _uiState.value.holding?.marketType ?: _uiState.value.marketType
        val history = getHoldingTransactionHistoryUseCase(code, marketType)
        _uiState.value = _uiState.value.copy(transactionHistory = history)
    }

    private suspend fun loadQuote() {
        when (_uiState.value.marketType) {
            MarketType.A_STOCK -> {
                val quotes = stockRepository.getAStockQuotes(listOf(code))
                val quote = quotes.firstOrNull()
                if (quote != null) {
                    _uiState.value = _uiState.value.copy(
                        stockQuote = quote,
                        name = quote.name,
                        holding = _uiState.value.holding?.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                        ),
                    )
                }
            }
            MarketType.HK_STOCK -> {
                val quotes = stockRepository.getHKStockQuotes(listOf(code))
                val quote = quotes.firstOrNull()
                if (quote != null) {
                    _uiState.value = _uiState.value.copy(
                        stockQuote = quote,
                        name = quote.name,
                        holding = _uiState.value.holding?.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                        ),
                    )
                }
            }
            MarketType.US_STOCK -> {
                val quotes = stockRepository.getUSStockQuotes(listOf(code))
                val quote = quotes.firstOrNull()
                if (quote != null) {
                    _uiState.value = _uiState.value.copy(
                        stockQuote = quote,
                        name = quote.name,
                        holding = _uiState.value.holding?.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                        ),
                    )
                }
            }
            MarketType.FUND -> {
                val quotes = fundRepository.getFundQuotes(listOf(code))
                val fq = quotes.firstOrNull()
                if (fq != null) {
                    _uiState.value = _uiState.value.copy(
                        fundQuote = fq,
                        name = fq.name,
                        holding = _uiState.value.holding?.copy(
                            currentPrice = fq.estimatedValue,
                            changePercent = fq.dailyChangePercent,
                        ),
                    )
                }
            }
            MarketType.GOLD -> {
                val quotes = stockRepository.getGoldQuotes(listOf(code))
                val quote = quotes.firstOrNull()
                if (quote != null) {
                    _uiState.value = _uiState.value.copy(
                        stockQuote = quote,
                        name = quote.name,
                        holding = _uiState.value.holding?.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun loadKLine(period: KLinePeriod) {
        _uiState.value = _uiState.value.copy(isKLineLoading = true)
        try {
            val data = if (_uiState.value.marketType == MarketType.FUND) {
                fundRepository.getFundNavHistory(code)
            } else {
                getKLineDataUseCase(code, period)
            }
            _uiState.value = _uiState.value.copy(kLineData = data, isKLineLoading = false)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(isKLineLoading = false)
        }
    }

    fun selectPeriod(period: KLinePeriod) {
        if (period == _uiState.value.selectedPeriod) return
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        kLineJob?.cancel()
        kLineJob = viewModelScope.launch {
            loadKLine(period)
        }
    }

    private fun loadNews() {
        val name = _uiState.value.name
        if (name.isBlank()) return
        val keyword = when (_uiState.value.marketType) {
            MarketType.GOLD -> "黄金行情"
            else -> name
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNewsLoading = true, newsError = null)
            try {
                val articles = newsRepository.searchNews(keyword)
                _uiState.value = _uiState.value.copy(
                    newsArticles = articles,
                    isNewsLoading = false,
                    newsLoaded = true,
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isNewsLoading = false,
                    newsArticles = emptyList(),
                    newsLoaded = true,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        kLineJob?.cancel()
    }

    companion object {
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_seconds"
        private const val DEFAULT_HKD_RATE = 0.92
    }
}
