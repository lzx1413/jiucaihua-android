package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.TransactionDao
import com.jiucaihua.app.data.local.entity.TransactionEntity
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeAll(): Flow<List<InvestmentTransaction>> {
        return transactionDao.observeAll().map { transactions ->
            transactions.map { it.toDomain() }
        }
    }

    override suspend fun getAllOnce(): List<InvestmentTransaction> {
        return transactionDao.getAllOnce().map { it.toDomain() }
    }

    override suspend fun getBySecurity(
        code: String,
        marketType: MarketType,
    ): List<InvestmentTransaction> {
        return transactionDao.getBySecurity(code, marketType.name).map { it.toDomain() }
    }

    override suspend fun query(query: TransactionQuery): List<InvestmentTransaction> {
        val limit = query.limit.coerceAtLeast(1)
        val offset = query.offset.coerceAtLeast(0)
        return transactionDao.query(
            code = query.code,
            marketType = query.marketType?.name,
            type = query.type?.name,
            from = query.from,
            to = query.to,
            limit = limit,
            offset = offset,
        ).map { it.toDomain() }
    }

    override suspend fun count(query: TransactionQuery): Int {
        return transactionDao.count(
            code = query.code,
            marketType = query.marketType?.name,
            type = query.type?.name,
            from = query.from,
            to = query.to,
        )
    }

    override suspend fun addTransaction(transaction: InvestmentTransaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun addTransactions(transactions: List<InvestmentTransaction>) {
        transactionDao.insertAll(transactions.map { it.toEntity() })
    }

    override suspend fun updateTransaction(transaction: InvestmentTransaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    override suspend fun clearAll() {
        transactionDao.clearAll()
    }

    private fun TransactionEntity.toDomain(): InvestmentTransaction {
        return InvestmentTransaction(
            id = id,
            code = code,
            name = name,
            marketType = marketType?.let { MarketType.valueOf(it) },
            type = TransactionType.valueOf(type),
            tradeDate = tradeDate,
            quantity = quantity,
            price = price,
            amount = amount,
            fee = fee,
            tax = tax,
            currency = currency,
            exchangeRate = exchangeRate,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun InvestmentTransaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            code = code,
            name = name,
            marketType = marketType?.name,
            type = type.name,
            tradeDate = tradeDate,
            quantity = quantity,
            price = price,
            amount = amount,
            fee = fee,
            tax = tax,
            currency = currency,
            exchangeRate = exchangeRate,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

}
