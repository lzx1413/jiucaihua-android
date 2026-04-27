package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.Holding
import kotlinx.coroutines.flow.Flow

interface HoldingRepository {
    fun getActiveHoldings(): Flow<List<Holding>>
    fun getAllHoldings(): Flow<List<Holding>>
    suspend fun getHoldingById(id: Long): Holding?
    suspend fun getHoldingByCode(code: String): Holding?
    suspend fun addHolding(holding: Holding): Long
    suspend fun updateHolding(holding: Holding)
    suspend fun deleteHolding(id: Long)
}
