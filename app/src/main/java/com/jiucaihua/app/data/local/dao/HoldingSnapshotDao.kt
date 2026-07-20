package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.HoldingSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingSnapshotDao {

    @Query("SELECT * FROM holding_snapshots ORDER BY timestamp ASC, id ASC")
    fun getAll(): Flow<List<HoldingSnapshotEntity>>

    @Query("SELECT * FROM holding_snapshots ORDER BY timestamp ASC, id ASC")
    suspend fun getAllOnce(): List<HoldingSnapshotEntity>

    @Query("SELECT * FROM holding_snapshots WHERE date = :date ORDER BY id ASC")
    suspend fun getByDate(date: String): List<HoldingSnapshotEntity>

    @Query("SELECT DISTINCT date FROM holding_snapshots WHERE code = :code AND marketType = :marketType")
    suspend fun getDatesBySecurity(code: String, marketType: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<HoldingSnapshotEntity>)

    @Query("DELETE FROM holding_snapshots WHERE code = :code AND marketType = :marketType")
    suspend fun deleteBySecurity(code: String, marketType: String)

    @Query("DELETE FROM holding_snapshots")
    suspend fun clearAll()
}
