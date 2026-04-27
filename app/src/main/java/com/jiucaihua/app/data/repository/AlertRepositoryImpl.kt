package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getAllAlerts(): Flow<List<PriceAlert>> {
        return alertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAlertsByCode(code: String): Flow<List<PriceAlert>> {
        return alertDao.getAlertsByCode(code).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEnabledAlerts(): List<PriceAlert> {
        return alertDao.getEnabledAlerts().map { it.toDomain() }
    }

    override suspend fun getAlertById(id: Long): PriceAlert? {
        return alertDao.getAlertById(id)?.toDomain()
    }

    override suspend fun addAlert(alert: PriceAlert): Long {
        return alertDao.insertAlert(alert.toEntity())
    }

    override suspend fun updateAlert(alert: PriceAlert) {
        alertDao.updateAlert(alert.toEntity())
    }

    override suspend fun setAlertEnabled(id: Long, isEnabled: Boolean) {
        alertDao.setAlertEnabled(id, isEnabled)
    }

    override suspend fun markTriggered(id: Long) {
        alertDao.setLastTriggered(id, System.currentTimeMillis())
    }

    override suspend fun deleteAlert(id: Long) {
        alertDao.deleteAlertById(id)
    }

    private fun AlertEntity.toDomain(): PriceAlert {
        return PriceAlert(
            id = id,
            code = code,
            name = name,
            alertType = AlertType.valueOf(alertType),
            threshold = threshold,
            isEnabled = isEnabled,
            lastTriggeredAt = lastTriggeredAt,
            createdAt = createdAt,
        )
    }

    private fun PriceAlert.toEntity(): AlertEntity {
        return AlertEntity(
            id = id,
            code = code,
            name = name,
            alertType = alertType.name,
            threshold = threshold,
            isEnabled = isEnabled,
            lastTriggeredAt = lastTriggeredAt,
            createdAt = createdAt,
        )
    }
}
