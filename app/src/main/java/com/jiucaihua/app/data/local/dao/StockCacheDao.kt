package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.StockCacheEntity

@Dao
interface StockCacheDao {

    @Query("SELECT * FROM stock_cache WHERE code = :code")
    suspend fun getByCode(code: String): StockCacheEntity?

    @Query("SELECT * FROM stock_cache WHERE code IN (:codes)")
    suspend fun getByCodes(codes: List<String>): List<StockCacheEntity>

    @Query("SELECT * FROM stock_cache")
    suspend fun getAllOnce(): List<StockCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<StockCacheEntity>)

    @Query("DELETE FROM stock_cache WHERE code = :code")
    suspend fun deleteByCode(code: String)

    @Query("DELETE FROM stock_cache")
    suspend fun clearAll()
}
