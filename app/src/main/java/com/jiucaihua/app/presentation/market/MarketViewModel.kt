package com.jiucaihua.app.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.MarketGroup
import com.jiucaihua.app.domain.model.MarketTab
import com.jiucaihua.app.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val groups: List<MarketGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val (aIndices, hkIndices, usIndices, goldIndices) = kotlinx.coroutines.coroutineScope {
                    val a = async { runCatching { marketRepository.getAStockIndices() }.getOrDefault(emptyList()) }
                    val hk = async { runCatching { marketRepository.getHKStockIndices() }.getOrDefault(emptyList()) }
                    val us = async { runCatching { marketRepository.getUSStockIndices() }.getOrDefault(emptyList()) }
                    val gold = async { runCatching { marketRepository.getGoldIndices() }.getOrDefault(emptyList()) }
                    FourResult(a.await(), hk.await(), us.await(), gold.await())
                }

                val groups = listOf(
                    MarketGroup(MarketTab.A_STOCK, aIndices),
                    MarketGroup(MarketTab.HK_STOCK, hkIndices),
                    MarketGroup(MarketTab.US_STOCK, usIndices),
                    MarketGroup(MarketTab.GOLD, goldIndices),
                ).filter { it.indices.isNotEmpty() }

                _uiState.update { it.copy(groups = groups, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "数据加载失败: ${e.message}", isLoading = false) }
            }
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                try {
                    loadData()
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
    }

    private data class FourResult<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val REFRESH_INTERVAL_MS = 15_000L
    }
}
