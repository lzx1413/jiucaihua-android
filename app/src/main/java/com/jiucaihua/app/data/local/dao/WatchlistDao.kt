package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY createdAt ASC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY createdAt ASC")
    suspend fun getAllWatchlistOnce(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE code = :code")
    suspend fun getWatchlistByCode(code: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchlistEntity>)

    @Query("DELETE FROM watchlist")
    suspend fun clearAll()

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT DISTINCT group_name FROM watchlist WHERE group_name != '' ORDER BY group_name ASC")
    fun getAllGroups(): Flow<List<String>>

    @Query("UPDATE watchlist SET group_name = :group WHERE id = :id")
    suspend fun updateGroup(id: Long, group: String)
}
