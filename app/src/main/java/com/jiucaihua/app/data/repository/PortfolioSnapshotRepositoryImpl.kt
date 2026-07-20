package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.PortfolioSnapshotDao
import com.jiucaihua.app.data.local.dao.HoldingSnapshotDao
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import com.jiucaihua.app.data.local.entity.HoldingSnapshotEntity
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioSnapshotRepositoryImpl @Inject constructor(
    private val dao: PortfolioSnapshotDao,
    private val holdingSnapshotDao: HoldingSnapshotDao,
) : PortfolioSnapshotRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAll(): Flow<List<PortfolioSnapshot>> {
        return combine(dao.getAll(), holdingSnapshotDao.getAll()) { portfolioSnapshots, holdingSnapshots ->
            mergeHoldingSnapshots(portfolioSnapshots, holdingSnapshots)
        }
    }

    override suspend fun getRange(from: Long, to: Long): List<PortfolioSnapshot> {
        return mergeHoldingSnapshots(dao.getRange(from, to), holdingSnapshotDao.getAllOnce())
    }

    override suspend fun getLatest(): PortfolioSnapshot? {
        val snapshot = dao.getLatest() ?: return null
        return mergeHoldingSnapshots(listOf(snapshot), holdingSnapshotDao.getByDate(snapshot.date)).firstOrNull()
    }

    override suspend fun getAllOnce(): List<PortfolioSnapshot> {
        return mergeHoldingSnapshots(dao.getAllOnce(), holdingSnapshotDao.getAllOnce())
    }

    override suspend fun saveSnapshot(snapshot: PortfolioSnapshot): Long {
        return dao.insert(snapshot.toEntity())
    }

    override suspend fun deleteBefore(beforeTimestamp: Long) {
        dao.deleteBefore(beforeTimestamp)
    }

    private fun PortfolioSnapshotEntity.toDomain(): PortfolioSnapshot {
        val categoryMap: Map<String, Double> = try {
            json.decodeFromString<Map<String, Double>>(categoryValuesJson)
        } catch (_: Exception) {
            emptyMap()
        }
        return PortfolioSnapshot(
            id = id,
            date = date,
            timestamp = timestamp,
            totalMarketValue = totalMarketValue,
            totalCost = totalCost,
            totalEarnings = totalEarnings,
            totalEarningsPercent = totalEarningsPercent,
            todayEarnings = todayEarnings,
            cash = cash,
            lossCompensation = lossCompensation,
            categoryValues = categoryMap,
            benchmarkPercent = benchmarkValue,
            netExternalCashFlow = netExternalCashFlow,
        )
    }

    private fun PortfolioSnapshot.toEntity(): PortfolioSnapshotEntity {
        return PortfolioSnapshotEntity(
            id = id,
            date = date,
            timestamp = timestamp,
            totalMarketValue = totalMarketValue,
            totalCost = totalCost,
            totalEarnings = totalEarnings,
            totalEarningsPercent = totalEarningsPercent,
            todayEarnings = todayEarnings,
            cash = cash,
            lossCompensation = lossCompensation,
            categoryValuesJson = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, Double>>(),
                categoryValues,
            ),
            benchmarkValue = benchmarkPercent,
            netExternalCashFlow = netExternalCashFlow,
        )
    }

    /**
     * Newer dates contain a per-security snapshot. Rebuild their aggregate values
     * from those rows; older dates keep the legacy aggregate as a compatibility fallback.
     */
    private fun mergeHoldingSnapshots(
        portfolioSnapshots: List<PortfolioSnapshotEntity>,
        holdingSnapshots: List<HoldingSnapshotEntity>,
    ): List<PortfolioSnapshot> {
        val holdingsByDate = holdingSnapshots.groupBy { it.date }
        return portfolioSnapshots.map { entity ->
            val base = entity.toDomain()
            val holdings = holdingsByDate[entity.date].orEmpty()
            if (holdings.isEmpty()) return@map base

            val marketValue = holdings.sumOf { it.marketValueCny }
            val cost = holdings.sumOf { it.costCny }
            val earnings = marketValue - cost
            base.copy(
                totalMarketValue = marketValue,
                totalCost = cost,
                totalEarnings = earnings,
                totalEarningsPercent = if (cost > 0.0) earnings / cost * 100.0 else 0.0,
                todayEarnings = holdings.sumOf { it.dailyEarningsCny },
                categoryValues = holdings.groupBy { it.marketType }
                    .mapValues { (_, snapshots) -> snapshots.sumOf { it.marketValueCny } },
            )
        }
    }
}
