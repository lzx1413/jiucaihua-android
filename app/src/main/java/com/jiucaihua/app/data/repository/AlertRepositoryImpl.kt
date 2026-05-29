package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.AlertRecordDao
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.AlertRecordEntity
import com.jiucaihua.app.domain.model.AlertRecord
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val alertRecordDao: AlertRecordDao,
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

    override fun getAlertRecords(): Flow<List<AlertRecord>> {
        return alertRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addAlertRecord(record: AlertRecord) {
        alertRecordDao.insertRecord(record.toEntity())
    }

    private fun AlertRecordEntity.toDomain(): AlertRecord {
        return AlertRecord(
            id = id,
            alertId = alertId,
            code = code,
            name = name,
            alertType = AlertType.valueOf(alertType),
            threshold = threshold,
            currentValue = currentValue,
            actionHint = actionHint,
            triggeredAt = triggeredAt,
        )
    }

    private fun AlertRecord.toEntity(): AlertRecordEntity {
        return AlertRecordEntity(
            id = id,
            alertId = alertId,
            code = code,
            name = name,
            alertType = alertType.name,
            threshold = threshold,
            currentValue = currentValue,
            actionHint = actionHint,
            triggeredAt = triggeredAt,
        )
    }

    private fun AlertEntity.toDomain(): PriceAlert {
        return PriceAlert(
            id = id,
            code = code,
            name = name,
            alertType = AlertType.valueOf(alertType),
            threshold = threshold,
            actionHint = actionHint,
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
            actionHint = actionHint,
            isEnabled = isEnabled,
            lastTriggeredAt = lastTriggeredAt,
            createdAt = createdAt,
        )
    }
}
