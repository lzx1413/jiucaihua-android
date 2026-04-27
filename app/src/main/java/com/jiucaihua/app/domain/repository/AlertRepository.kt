package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.PriceAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getAllAlerts(): Flow<List<PriceAlert>>
    fun getAlertsByCode(code: String): Flow<List<PriceAlert>>
    suspend fun getEnabledAlerts(): List<PriceAlert>
    suspend fun getAlertById(id: Long): PriceAlert?
    suspend fun addAlert(alert: PriceAlert): Long
    suspend fun updateAlert(alert: PriceAlert)
    suspend fun setAlertEnabled(id: Long, isEnabled: Boolean)
    suspend fun markTriggered(id: Long)
    suspend fun deleteAlert(id: Long)
}
