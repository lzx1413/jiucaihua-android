package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jiucaihua.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY tradeDate DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY tradeDate ASC, id ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE code = :code AND marketType = :marketType ORDER BY tradeDate ASC, id ASC")
    suspend fun getBySecurity(code: String, marketType: String): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:code IS NULL OR code = :code)
          AND (:marketType IS NULL OR marketType = :marketType)
          AND (:type IS NULL OR type = :type)
          AND (:from IS NULL OR tradeDate >= :from)
          AND (:to IS NULL OR tradeDate <= :to)
        ORDER BY tradeDate DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun query(
        code: String?,
        marketType: String?,
        type: String?,
        from: Long?,
        to: Long?,
        limit: Int,
        offset: Int,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE (:code IS NULL OR code = :code)
          AND (:marketType IS NULL OR marketType = :marketType)
          AND (:type IS NULL OR type = :type)
          AND (:from IS NULL OR tradeDate >= :from)
          AND (:to IS NULL OR tradeDate <= :to)
        """
    )
    suspend fun count(
        code: String?,
        marketType: String?,
        type: String?,
        from: Long?,
        to: Long?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
