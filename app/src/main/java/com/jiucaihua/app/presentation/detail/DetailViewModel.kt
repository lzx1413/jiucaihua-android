package com.jiucaihua.app.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
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
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
    private val holdingRepository: HoldingRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val getKLineDataUseCase: GetKLineDataUseCase,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
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
                loadQuote()
                loadKLine(_uiState.value.selectedPeriod)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "数据加载失败: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
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
        val marketType = MarketType.fromCode(code)
        val exchangeRate = if (marketType == MarketType.HK_STOCK) {
            try { exchangeRateRepository.getHkdToCnyRate() } catch (_: Exception) { 0.92 }
        } else 1.0
        _uiState.value = _uiState.value.copy(
            holding = holding.copy(exchangeRate = exchangeRate),
            name = holding.name,
        )
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

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        kLineJob?.cancel()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 10_000L
    }
}
