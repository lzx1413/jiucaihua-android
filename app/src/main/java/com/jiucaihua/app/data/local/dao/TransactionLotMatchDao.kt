package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.TransactionLotMatchEntity

@Dao
interface TransactionLotMatchDao {

    @Query("SELECT * FROM transaction_lot_matches WHERE code = :code AND marketType = :marketType ORDER BY sellTransactionId ASC, id ASC")
    suspend fun getBySecurity(code: String, marketType: String): List<TransactionLotMatchEntity>

    @Query("SELECT * FROM transaction_lot_matches WHERE sellTransactionId IN (:sellTransactionIds) ORDER BY sellTransactionId ASC, id ASC")
    suspend fun getBySellTransactionIds(sellTransactionIds: List<Long>): List<TransactionLotMatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<TransactionLotMatchEntity>)

    @Query("DELETE FROM transaction_lot_matches WHERE code = :code AND marketType = :marketType")
    suspend fun deleteBySecurity(code: String, marketType: String)

    @Query("DELETE FROM transaction_lot_matches")
    suspend fun clearAll()
}
