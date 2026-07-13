package com.jiucaihua.app.presentation.holdings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import com.jiucaihua.app.domain.usecase.AddTransactionUseCase
import com.jiucaihua.app.domain.usecase.ManageHoldingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HoldingTradeAction(val label: String) {
    BUY("增加"),
    SELL("减少"),
    DIVIDEND("分红"),
}

data class AddEditHoldingUiState(
    val isEditing: Boolean = false,
    val holdingId: Long = -1L,
    val code: String = "",
    val name: String = "",
    val marketType: MarketType = MarketType.A_STOCK,
    val costPrice: String = "",
    val holdingShares: String = "",
    val tradeAction: HoldingTradeAction = HoldingTradeAction.BUY,
    val originalCostPrice: Double = 0.0,
    val originalHoldingShares: Double = 0.0,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val searchExpanded: Boolean = false,
    val searchResults: List<SecuritySearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
) {
    val holdingAmount: Double
        get() {
            val price = costPrice.toDoubleOrNull() ?: 0.0
            val shares = holdingShares.toDoubleOrNull() ?: 0.0
            return price * shares
        }

    val isValid: Boolean
        get() {
            val price = costPrice.toDoubleOrNull() ?: 0.0
            val shares = holdingShares.toDoubleOrNull() ?: 0.0
            if (isEditing && tradeAction == HoldingTradeAction.DIVIDEND) {
                return code.isNotBlank() && name.isNotBlank() && price > 0
            }
            return code.isNotBlank() && name.isNotBlank() &&
                    price > 0 &&
                    shares > 0 &&
                    (!isEditing || tradeAction == HoldingTradeAction.BUY || shares <= originalHoldingShares)
        }
}

@HiltViewModel
class AddEditHoldingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val manageHoldingUseCase: ManageHoldingUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val securitySearchRepository: SecuritySearchRepository,
) : ViewModel() {

    private val holdingId: Long = savedStateHandle.get<Long>("holdingId") ?: -1L
    private var originalHolding: Holding? = null
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(AddEditHoldingUiState())
    val uiState: StateFlow<AddEditHoldingUiState> = _uiState.asStateFlow()

    init {
        if (holdingId > 0) {
            loadHolding(holdingId)
        }
    }

    private fun loadHolding(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val holding = manageHoldingUseCase.getHoldingById(id)
            if (holding != null) {
                originalHolding = holding
                _uiState.update {
                    it.copy(
                        isEditing = true,
                        holdingId = holding.id,
                        code = holding.code,
                        name = holding.name,
                        marketType = holding.marketType,
                        costPrice = "",
                        holdingShares = "",
                        originalCostPrice = holding.costPrice,
                        originalHoldingShares = holding.holdingShares,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.holding_not_found),
                    )
                }
            }
        }
    }

    fun onCodeChange(value: String) {
        if (_uiState.value.marketType == MarketType.GOLD) {
            _uiState.update { it.copy(code = value) }
            return
        }

        val query = value.trim()
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    code = value,
                    name = "",
                    searchError = null,
                    searchResults = emptyList(),
                    isSearching = false,
                    searchExpanded = false,
                )
            }
            return
        }

        _uiState.update { it.copy(code = value, name = "", searchError = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onMarketTypeChange(type: MarketType) {
        _uiState.update { it.copy(marketType = type) }
    }

    fun onCostPriceChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(costPrice = value) }
        }
    }

    fun onHoldingSharesChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(holdingShares = value) }
        }
    }

    fun onTradeActionChange(action: HoldingTradeAction) {
        _uiState.update { it.copy(tradeAction = action) }
    }

    fun applySearchResult(result: SecuritySearchResult) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                code = result.code,
                name = result.name,
                marketType = result.marketType,
            )
        }
        resetSearchState()
    }

    fun selectGoldPreset(code: String, name: String) {
        _uiState.update { it.copy(code = code, name = name) }
    }

    fun dismissSearch() {
        searchJob?.cancel()
        resetSearchState()
    }

    private fun resetSearchState() {
        _uiState.update {
            it.copy(
                searchExpanded = false,
                searchResults = emptyList(),
                isSearching = false,
                searchError = null,
            )
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, searchError = null, searchExpanded = true) }
        try {
            val results = securitySearchRepository.search(query)
            val currentState = _uiState.value
            if (currentState.code.trim() != query) return

            _uiState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    searchError = null,
                    searchExpanded = results.isNotEmpty(),
                )
            }
        } catch (_: Exception) {
            val currentState = _uiState.value
            if (currentState.code.trim() != query) return

            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    searchError = context.getString(R.string.search_failed_retry),
                    searchExpanded = false,
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentHolding = originalHolding
            val transactionType = when {
                state.isEditing && state.tradeAction == HoldingTradeAction.SELL -> TransactionType.SELL
                state.isEditing && state.tradeAction == HoldingTradeAction.DIVIDEND -> TransactionType.DIVIDEND
                else -> TransactionType.BUY
            }
            val amount = if (transactionType == TransactionType.DIVIDEND) {
                state.costPrice.toDouble()
            } else {
                state.holdingAmount
            }
            val transaction = InvestmentTransaction(
                code = state.code.trim(),
                name = state.name.trim(),
                marketType = state.marketType,
                type = transactionType,
                tradeDate = System.currentTimeMillis(),
                quantity = if (transactionType == TransactionType.DIVIDEND) 0.0 else state.holdingShares.toDouble(),
                price = if (transactionType == TransactionType.DIVIDEND) 0.0 else state.costPrice.toDouble(),
                amount = amount,
                currency = currencyFor(state.marketType),
                exchangeRate = exchangeRateFor(state.marketType),
                note = when {
                    !state.isEditing -> context.getString(R.string.add_holding)
                    transactionType == TransactionType.BUY -> context.getString(R.string.trade_buy)
                    transactionType == TransactionType.DIVIDEND -> context.getString(R.string.trade_dividend)
                    else -> context.getString(R.string.trade_sell)
                },
            )
            if (state.isEditing) {
                if (currentHolding == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = context.getString(R.string.holding_not_found),
                        )
                    }
                    return@launch
                }
            }
            addTransactionUseCase(transaction)
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    private fun currencyFor(marketType: MarketType): String {
        return when (marketType) {
            MarketType.HK_STOCK -> "HKD"
            MarketType.US_STOCK -> "USD"
            else -> "CNY"
        }
    }

    private fun exchangeRateFor(marketType: MarketType): Double {
        return when (marketType) {
            MarketType.HK_STOCK -> DEFAULT_HKD_RATE
            MarketType.US_STOCK -> DEFAULT_USD_RATE
            else -> 1.0
        }
    }

    companion object {
        private const val DEFAULT_HKD_RATE = 0.92
        private const val DEFAULT_USD_RATE = 7.2
    }
}
