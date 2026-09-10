package com.jiucaihua.app.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jiucaihua.app.data.local.entity.TransactionEntity
import com.jiucaihua.app.data.repository.TransactionRepositoryImpl
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.usecase.GetTransactionSummaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioCashFlowQueryTest {
    @Test fun aggregateMatchesExistingAnalysisWindowAndCurrencyConversion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.transactionDao()
            assertEquals(0.0, dao.getNetExternalCashFlow(null, 5000), 0.000001)
            dao.insertAll((1..6000).map { index ->
                TransactionEntity(
                    id = index.toLong(),
                    type = when (index % 3) { 0 -> "CASH_IN"; 1 -> "CASH_OUT"; else -> "DIVIDEND" },
                    tradeDate = (index / 2).toLong(), // Ties verify that the id ordering is preserved.
                    amount = index.toDouble(),
                    currency = "USD",
                    exchangeRate = 7.1,
                )
            })
            val repository = TransactionRepositoryImpl(dao)
            val summary = GetTransactionSummaryUseCase(repository)
            for (to in listOf(null, 2500L, 0L)) {
                val old = summary(TransactionQuery(to = to))
                assertEquals(old.cashInCny - old.cashOutCny, summary.getNetExternalCashFlow(to), 0.00001)
            }
            val allCash = repository.getAllOnce().filter { it.type.name in listOf("CASH_IN", "CASH_OUT") }
            assertEquals(allCash, repository.getExternalCashFlows())
            // The period-return query intentionally has no 5000-row truncation.
            assertEquals(4000, allCash.size)
        } finally {
            db.close()
        }
    }
}
