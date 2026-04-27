package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jiucaihua.app.data.local.entity.HoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {

    @Query("SELECT * FROM holdings WHERE isSoldOut = 0 ORDER BY updatedAt DESC")
    fun getActiveHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings ORDER BY updatedAt DESC")
    fun getAllHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE id = :id")
    suspend fun getHoldingById(id: Long): HoldingEntity?

    @Query("SELECT * FROM holdings WHERE code = :code AND isSoldOut = 0")
    suspend fun getHoldingByCode(code: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: HoldingEntity): Long

    @Update
    suspend fun updateHolding(holding: HoldingEntity)

    @Delete
    suspend fun deleteHolding(holding: HoldingEntity)

    @Query("DELETE FROM holdings WHERE id = :id")
    suspend fun deleteHoldingById(id: Long)
}
