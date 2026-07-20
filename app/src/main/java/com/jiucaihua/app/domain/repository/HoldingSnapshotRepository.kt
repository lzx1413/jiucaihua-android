package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.HoldingSnapshot
import kotlinx.coroutines.flow.Flow

interface HoldingSnapshotRepository {
    fun observeAll(): Flow<List<HoldingSnapshot>>
    suspend fun getAllOnce(): List<HoldingSnapshot>
    suspend fun saveSnapshots(date: String, timestamp: Long, holdings: List<Holding>)
}
