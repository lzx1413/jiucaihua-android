package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.TransactionSummary
import com.jiucaihua.app.domain.repository.HoldingSnapshotRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ResetPortfolioReferenceDateUseCaseTest {

    @Test
    fun `clears return snapshots and saves the current wealth as the new reference`() = runTest {
        val portfolioUseCase = mock<GetPortfolioUseCase>()
        val transactionSummaryUseCase = mock<GetTransactionSummaryUseCase>()
        val holdingSnapshotRepository = mock<HoldingSnapshotRepository>()
        val snapshotRepository = mock<PortfolioSnapshotRepository>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(portfolioUseCase.getPortfolioWithQuotes()).thenReturn(
            PortfolioSummary(
                totalMarketValue = 12_000.0,
                totalCost = 10_000.0,
                totalEarnings = 2_000.0,
                totalEarningsPercent = 20.0,
                todayEarnings = 100.0,
                cash = 3_000.0,
                holdings = listOf(
                    Holding(
                        code = "600000",
                        name = "测试股票",
                        marketType = MarketType.A_STOCK,
                        holdingShares = 100.0,
                        costPrice = 100.0,
                        holdingAmount = 10_000.0,
                    ),
                ),
            ),
        )
        whenever(transactionSummaryUseCase()).thenReturn(
            TransactionSummary(cashInCny = 20_000.0, cashOutCny = 5_000.0),
        )

        val result = ResetPortfolioReferenceDateUseCase(
            getPortfolioUseCase = portfolioUseCase,
            getTransactionSummaryUseCase = transactionSummaryUseCase,
            holdingSnapshotRepository = holdingSnapshotRepository,
            snapshotRepository = snapshotRepository,
            prefs = prefs,
        )()

        val snapshotCaptor = argumentCaptor<com.jiucaihua.app.domain.model.PortfolioSnapshot>()
        verify(snapshotRepository).clearAll()
        verify(holdingSnapshotRepository).saveSnapshots(any(), any(), any())
        verify(snapshotRepository).saveSnapshot(snapshotCaptor.capture())
        verify(editor).remove("loss_compensation")
        verify(editor).remove("benchmark_base_csi300")
        verify(editor).putLong(org.mockito.kotlin.eq("portfolio_reference_reset_at"), any())
        verify(editor).apply()
        assertTrue(result)
        assertEquals(12_000.0, snapshotCaptor.firstValue.totalMarketValue, 0.0001)
        assertEquals(3_000.0, snapshotCaptor.firstValue.cash, 0.0001)
        assertEquals(15_000.0, snapshotCaptor.firstValue.netExternalCashFlow, 0.0001)
        assertEquals(0.0, snapshotCaptor.firstValue.lossCompensation, 0.0001)
    }
}
