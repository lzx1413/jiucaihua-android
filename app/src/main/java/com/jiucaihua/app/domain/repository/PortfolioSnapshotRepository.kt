package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.PortfolioSnapshot
import kotlinx.coroutines.flow.Flow

interface PortfolioSnapshotRepository {

    fun observeAll(): Flow<List<PortfolioSnapshot>>

    suspend fun getRange(from: Long, to: Long): List<PortfolioSnapshot>

    suspend fun getLatest(): PortfolioSnapshot?

    suspend fun getAllOnce(): List<PortfolioSnapshot>

    suspend fun saveSnapshot(snapshot: PortfolioSnapshot): Long

    /** Clears portfolio and holding snapshots while keeping transaction history intact. */
    suspend fun clearAll()

    suspend fun deleteBefore(beforeTimestamp: Long)
}
