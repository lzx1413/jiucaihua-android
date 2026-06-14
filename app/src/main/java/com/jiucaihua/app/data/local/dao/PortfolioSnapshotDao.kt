package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp ASC")
    fun getAll(): Flow<List<PortfolioSnapshotEntity>>

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<PortfolioSnapshotEntity>

    @Query("SELECT * FROM portfolio_snapshots WHERE timestamp >= :from AND timestamp <= :to ORDER BY timestamp ASC")
    suspend fun getRange(from: Long, to: Long): List<PortfolioSnapshotEntity>

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): PortfolioSnapshotEntity?

    @Query("SELECT * FROM portfolio_snapshots WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): PortfolioSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: PortfolioSnapshotEntity): Long

    @Query("DELETE FROM portfolio_snapshots WHERE timestamp < :beforeTimestamp")
    suspend fun deleteBefore(beforeTimestamp: Long): Int
}
