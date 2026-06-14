package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.PortfolioSnapshotDao
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioSnapshotRepositoryImpl @Inject constructor(
    private val dao: PortfolioSnapshotDao,
) : PortfolioSnapshotRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAll(): Flow<List<PortfolioSnapshot>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRange(from: Long, to: Long): List<PortfolioSnapshot> {
        return dao.getRange(from, to).map { it.toDomain() }
    }

    override suspend fun getLatest(): PortfolioSnapshot? {
        return dao.getLatest()?.toDomain()
    }

    override suspend fun getAllOnce(): List<PortfolioSnapshot> {
        return dao.getAllOnce().map { it.toDomain() }
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
        )
    }
}
