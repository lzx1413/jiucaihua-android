package com.jiucaihua.app.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.FundFlowData
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketIndex
import com.jiucaihua.app.domain.model.MarketIndexCodes
import com.jiucaihua.app.domain.model.MarketTab
import com.jiucaihua.app.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val currentTab: MarketTab = MarketTab.A_STOCK,
    val indices: List<MarketIndex> = emptyList(),
    val selectedIndex: MarketIndex? = null,
    val kLineData: KLineData? = null,
    val selectedPeriod: KLinePeriod = KLinePeriod.DAILY,
    val fundFlowData: FundFlowData = FundFlowData(),
    val isLoading: Boolean = true,
    val isKLineLoading: Boolean = false,
    val isFundFlowLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var kLineJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                loadIndices()
                loadFundFlow()
                loadKLine()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "数据加载失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadIndices() {
        val indices = when (_uiState.value.currentTab) {
            MarketTab.A_STOCK -> marketRepository.getAStockIndices()
            MarketTab.HK_STOCK -> marketRepository.getHKStockIndices()
            MarketTab.US_STOCK -> marketRepository.getUSStockIndices()
        }

        val selectedIndex = if (_uiState.value.selectedIndex == null) {
            indices.firstOrNull()
        } else {
            indices.find { it.code == _uiState.value.selectedIndex!!.code } ?: indices.firstOrNull()
        }

        _uiState.update { it.copy(indices = indices, selectedIndex = selectedIndex) }
    }

    private suspend fun loadFundFlow() {
        if (_uiState.value.currentTab != MarketTab.A_STOCK) {
            _uiState.update { it.copy(fundFlowData = FundFlowData()) }
            return
        }

        _uiState.update { it.copy(isFundFlowLoading = true) }
        try {
            val data = marketRepository.getFundFlowData()
            _uiState.update { it.copy(fundFlowData = data, isFundFlowLoading = false) }
        } catch (_: Exception) {
            _uiState.update { it.copy(isFundFlowLoading = false) }
        }
    }

    private suspend fun loadKLine() {
        val selectedCode = _uiState.value.selectedIndex?.code
        if (selectedCode == null) return

        if (selectedCode.startsWith("usr_")) {
            _uiState.update { it.copy(kLineData = null, isKLineLoading = false) }
            return
        }

        _uiState.update { it.copy(isKLineLoading = true) }
        try {
            val data = marketRepository.getIndexKLineData(selectedCode, _uiState.value.selectedPeriod)
            _uiState.update { it.copy(kLineData = data, isKLineLoading = false) }
        } catch (_: Exception) {
            _uiState.update { it.copy(kLineData = null, isKLineLoading = false) }
        }
    }

    fun selectTab(tab: MarketTab) {
        if (tab == _uiState.value.currentTab) return

        refreshJob?.cancel()
        kLineJob?.cancel()

        _uiState.update {
            MarketUiState(
                currentTab = tab,
                selectedIndex = null,
                isLoading = true,
            )
        }

        viewModelScope.launch {
            try {
                loadIndices()
                loadFundFlow()
                loadKLine()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "数据加载失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectIndex(index: MarketIndex) {
        if (index.code == _uiState.value.selectedIndex?.code) return

        _uiState.update { it.copy(selectedIndex = index, kLineData = null) }
        kLineJob?.cancel()
        kLineJob = viewModelScope.launch {
            loadKLine()
        }
    }

    fun selectPeriod(period: KLinePeriod) {
        if (period == _uiState.value.selectedPeriod) return

        _uiState.update { it.copy(selectedPeriod = period) }
        kLineJob?.cancel()
        kLineJob = viewModelScope.launch {
            loadKLine()
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                try {
                    loadIndices()
                    if (_uiState.value.currentTab == MarketTab.A_STOCK) {
                        loadFundFlow()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        kLineJob?.cancel()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15_000L
    }
}