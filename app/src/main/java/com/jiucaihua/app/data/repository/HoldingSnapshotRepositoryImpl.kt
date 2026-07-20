package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.HoldingSnapshotDao
import com.jiucaihua.app.data.local.entity.HoldingSnapshotEntity
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.HoldingSnapshot
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.HoldingSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoldingSnapshotRepositoryImpl @Inject constructor(
    private val dao: HoldingSnapshotDao,
) : HoldingSnapshotRepository {

    override fun observeAll(): Flow<List<HoldingSnapshot>> = dao.getAll().map { snapshots ->
        snapshots.map { it.toDomain() }
    }

    override suspend fun getAllOnce(): List<HoldingSnapshot> = dao.getAllOnce().map { it.toDomain() }

    override suspend fun saveSnapshots(date: String, timestamp: Long, holdings: List<Holding>) {
        dao.insertAll(holdings.map { holding ->
            HoldingSnapshotEntity(
                code = holding.code,
                name = holding.name,
                marketType = holding.marketType.name,
                date = date,
                timestamp = timestamp,
                holdingShares = holding.holdingShares,
                currentPrice = holding.currentPrice,
                costPrice = holding.costPrice,
                exchangeRate = holding.exchangeRate,
                marketValueCny = holding.marketValueCNY,
                costCny = holding.costPrice * holding.holdingShares * holding.exchangeRate,
                dailyEarningsCny = holding.dailyEarningsCNY,
            )
        })
    }

    private fun HoldingSnapshotEntity.toDomain() = HoldingSnapshot(
        id = id,
        code = code,
        name = name,
        marketType = MarketType.valueOf(marketType),
        date = date,
        timestamp = timestamp,
        holdingShares = holdingShares,
        currentPrice = currentPrice,
        costPrice = costPrice,
        exchangeRate = exchangeRate,
        marketValueCny = marketValueCny,
        costCny = costCny,
        dailyEarningsCny = dailyEarningsCny,
    )
}
