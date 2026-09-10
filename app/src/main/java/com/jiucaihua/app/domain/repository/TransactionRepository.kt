package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionQuery
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<InvestmentTransaction>>
    suspend fun getAllOnce(): List<InvestmentTransaction>
    suspend fun getExternalCashFlows(): List<InvestmentTransaction>
    suspend fun getBySecurity(code: String, marketType: MarketType): List<InvestmentTransaction>
    suspend fun query(query: TransactionQuery): List<InvestmentTransaction>
    suspend fun count(query: TransactionQuery): Int
    /** Cash in minus cash out over the same latest-row window used by transaction analysis. */
    suspend fun getNetExternalCashFlow(to: Long?, limit: Int): Double
    suspend fun addTransaction(transaction: InvestmentTransaction): Long
    suspend fun addTransactions(transactions: List<InvestmentTransaction>)
    suspend fun updateTransaction(transaction: InvestmentTransaction)
    suspend fun deleteTransaction(id: Long)
    suspend fun clearAll()
}
