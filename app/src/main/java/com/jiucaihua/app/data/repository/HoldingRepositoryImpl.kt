package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.HoldingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoldingRepositoryImpl @Inject constructor(
    private val holdingDao: HoldingDao
) : HoldingRepository {

    override fun getActiveHoldings(): Flow<List<Holding>> {
        return holdingDao.getActiveHoldings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllHoldings(): Flow<List<Holding>> {
        return holdingDao.getAllHoldings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getHoldingById(id: Long): Holding? {
        return holdingDao.getHoldingById(id)?.toDomain()
    }

    override suspend fun getHoldingByCode(code: String): Holding? {
        return holdingDao.getHoldingByCode(code)?.toDomain()
    }

    override suspend fun addHolding(holding: Holding): Long {
        return holdingDao.insertHolding(holding.toEntity())
    }

    override suspend fun updateHolding(holding: Holding) {
        holdingDao.updateHolding(holding.toEntity())
    }

    override suspend fun deleteHolding(id: Long) {
        holdingDao.deleteHoldingById(id)
    }

    private fun HoldingEntity.toDomain(): Holding {
        return Holding(
            id = id,
            code = code,
            name = name,
            marketType = MarketType.valueOf(marketType),
            currency = currency,
            exchangeRate = if (currency == "HKD") 0.92 else 1.0,
            costPrice = costPrice,
            holdingAmount = holdingAmount,
            holdingShares = holdingShares,
            isSoldOut = isSoldOut,
        )
    }

    private fun Holding.toEntity(): HoldingEntity {
        return HoldingEntity(
            id = id,
            code = code,
            name = name,
            marketType = marketType.name,
            currency = currency,
            costPrice = costPrice,
            holdingAmount = holdingAmount,
            holdingShares = holdingShares,
            isSoldOut = isSoldOut,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
