package com.jiucaihua.app.presentation.holdings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import com.jiucaihua.app.domain.usecase.ManageHoldingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditHoldingUiState(
    val isEditing: Boolean = false,
    val holdingId: Long = -1L,
    val code: String = "",
    val name: String = "",
    val marketType: MarketType = MarketType.A_STOCK,
    val costPrice: String = "",
    val holdingShares: String = "",
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
        get() = code.isNotBlank() && name.isNotBlank() &&
                (costPrice.toDoubleOrNull() ?: 0.0) > 0 &&
                (holdingShares.toDoubleOrNull() ?: 0.0) > 0
}

@HiltViewModel
class AddEditHoldingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val manageHoldingUseCase: ManageHoldingUseCase,
    private val securitySearchRepository: SecuritySearchRepository,
) : ViewModel() {

    private val holdingId: Long = savedStateHandle.get<Long>("holdingId") ?: -1L
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
                _uiState.update {
                    it.copy(
                        isEditing = true,
                        holdingId = holding.id,
                        code = holding.code,
                        name = holding.name,
                        marketType = holding.marketType,
                        costPrice = holding.costPrice.toString(),
                        holdingShares = holding.holdingShares.toString(),
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "持仓记录不存在") }
            }
        }
    }

    fun onCodeChange(value: String) {
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
                    searchError = "搜索失败，请稍后重试",
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
            val currency = if (state.marketType == MarketType.HK_STOCK) "HKD" else "CNY"
            val holding = Holding(
                id = if (state.isEditing) state.holdingId else 0,
                code = state.code.trim(),
                name = state.name.trim(),
                marketType = state.marketType,
                currency = currency,
                costPrice = state.costPrice.toDouble(),
                holdingAmount = state.holdingAmount,
                holdingShares = state.holdingShares.toDouble(),
            )
            if (state.isEditing) {
                manageHoldingUseCase.updateHolding(holding)
            } else {
                manageHoldingUseCase.addHolding(holding)
            }
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
