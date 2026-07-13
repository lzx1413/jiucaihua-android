package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.TransactionLotMatchDao
import com.jiucaihua.app.data.local.entity.TransactionLotMatchEntity
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionLotMatch
import com.jiucaihua.app.domain.repository.TransactionLotMatchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionLotMatchRepositoryImpl @Inject constructor(
    private val dao: TransactionLotMatchDao,
) : TransactionLotMatchRepository {

    override suspend fun getBySecurity(code: String, marketType: MarketType): List<TransactionLotMatch> {
        return dao.getBySecurity(code, marketType.name).map { it.toDomain() }
    }

    override suspend fun getBySellTransactionIds(sellTransactionIds: List<Long>): List<TransactionLotMatch> {
        if (sellTransactionIds.isEmpty()) return emptyList()
        return dao.getBySellTransactionIds(sellTransactionIds).map { it.toDomain() }
    }

    override suspend fun replaceForSecurity(
        code: String,
        marketType: MarketType,
        matches: List<TransactionLotMatch>,
    ) {
        dao.deleteBySecurity(code, marketType.name)
        dao.insertAll(matches.map { it.toEntity() })
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    private fun TransactionLotMatchEntity.toDomain(): TransactionLotMatch {
        return TransactionLotMatch(
            id = id,
            code = code,
            marketType = MarketType.valueOf(marketType),
            sellTransactionId = sellTransactionId,
            buyTransactionId = buyTransactionId,
            quantity = quantity,
            buyUnitCostCny = buyUnitCostCny,
            sellUnitProceedsCny = sellUnitProceedsCny,
            costBasisCny = costBasisCny,
            proceedsCny = proceedsCny,
            realizedPnlCny = realizedPnlCny,
            createdAt = createdAt,
        )
    }

    private fun TransactionLotMatch.toEntity(): TransactionLotMatchEntity {
        return TransactionLotMatchEntity(
            id = id,
            code = code,
            marketType = marketType.name,
            sellTransactionId = sellTransactionId,
            buyTransactionId = buyTransactionId,
            quantity = quantity,
            buyUnitCostCny = buyUnitCostCny,
            sellUnitProceedsCny = sellUnitProceedsCny,
            costBasisCny = costBasisCny,
            proceedsCny = proceedsCny,
            realizedPnlCny = realizedPnlCny,
            createdAt = createdAt,
        )
    }
}
