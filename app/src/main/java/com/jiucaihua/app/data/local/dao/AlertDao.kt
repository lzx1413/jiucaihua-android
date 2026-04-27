package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jiucaihua.app.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE code = :code ORDER BY createdAt DESC")
    fun getAlertsByCode(code: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isEnabled = 1")
    suspend fun getEnabledAlerts(): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: Long): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlertEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE alerts SET lastTriggeredAt = :triggeredAt WHERE id = :id")
    suspend fun setLastTriggered(id: Long, triggeredAt: Long)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)
}
