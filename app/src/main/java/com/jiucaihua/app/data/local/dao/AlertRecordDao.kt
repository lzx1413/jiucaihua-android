package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.AlertRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertRecordDao {

    @Query("SELECT * FROM alert_records ORDER BY triggeredAt DESC")
    fun getAllRecords(): Flow<List<AlertRecordEntity>>

    @Insert
    suspend fun insertRecord(record: AlertRecordEntity): Long

    @Query("DELETE FROM alert_records WHERE triggeredAt < :beforeTimestamp")
    suspend fun deleteRecordsBefore(beforeTimestamp: Long): Int

    @Query("DELETE FROM alert_records")
    suspend fun clearAllRecords()
}
