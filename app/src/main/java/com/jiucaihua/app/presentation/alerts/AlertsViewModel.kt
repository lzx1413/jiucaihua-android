package com.jiucaihua.app.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<PriceAlert> = emptyList(),
    val records: List<AlertRecord> = emptyList(),
    val holdings: List<Holding> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingAlert: PriceAlert? = null,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val getPortfolioUseCase: GetPortfolioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        observeAlerts()
        observeRecords()
        loadHoldings()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            alertRepository.getAllAlerts().collect { alerts ->
                _uiState.value = _uiState.value.copy(
                    alerts = alerts,
                    isLoading = false,
                )
            }
        }
    }

    private fun observeRecords() {
        viewModelScope.launch {
            alertRepository.getAlertRecords().collect { records ->
                _uiState.value = _uiState.value.copy(records = records)
            }
        }
    }

    private fun loadHoldings() {
        viewModelScope.launch {
            val holdings = getPortfolioUseCase.observeHoldings().first()
            _uiState.value = _uiState.value.copy(holdings = holdings)
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addAlert(code: String, name: String, alertType: AlertType, threshold: Double, actionHint: String?) {
        viewModelScope.launch {
            alertRepository.addAlert(
                PriceAlert(
                    code = code,
                    name = name,
                    alertType = alertType,
                    threshold = threshold,
                    actionHint = actionHint,
                )
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun toggleAlert(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            alertRepository.setAlertEnabled(id, isEnabled)
        }
    }

    fun showEditDialog(alert: PriceAlert) {
        _uiState.value = _uiState.value.copy(editingAlert = alert)
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(editingAlert = null)
    }

    fun updateAlert(id: Long, code: String, name: String, alertType: AlertType, threshold: Double, actionHint: String?) {
        viewModelScope.launch {
            val existing = alertRepository.getAlertById(id) ?: return@launch
            alertRepository.updateAlert(
                existing.copy(
                    code = code,
                    name = name,
                    alertType = alertType,
                    threshold = threshold,
                    actionHint = actionHint,
                )
            )
            _uiState.value = _uiState.value.copy(editingAlert = null)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            alertRepository.deleteAlert(id)
        }
    }
}
