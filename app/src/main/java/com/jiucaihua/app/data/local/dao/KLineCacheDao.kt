package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.KLineCacheEntity

@Dao
interface KLineCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(points: List<KLineCacheEntity>)

    @Query("SELECT * FROM kline_cache WHERE code = :code AND period = :period AND adjustment = :adjustment AND provider = :provider ORDER BY date ASC")
    suspend fun getPoints(code: String, period: String, adjustment: String, provider: String): List<KLineCacheEntity>

    @Query("DELETE FROM kline_cache WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
