package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.FundCacheEntity

@Dao
interface FundCacheDao {

    @Query("SELECT * FROM fund_cache WHERE code = :code")
    suspend fun getByCode(code: String): FundCacheEntity?

    @Query("SELECT * FROM fund_cache WHERE code IN (:codes)")
    suspend fun getByCodes(codes: List<String>): List<FundCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: FundCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<FundCacheEntity>)

    @Query("DELETE FROM fund_cache WHERE code = :code")
    suspend fun deleteByCode(code: String)
}
