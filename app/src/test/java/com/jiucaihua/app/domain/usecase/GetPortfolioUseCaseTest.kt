package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.data.cache.GoldYestCloseCache
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import com.jiucaihua.app.domain.repository.StockRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetPortfolioUseCaseTest {

    private lateinit var holdingRepository: HoldingRepository
    private lateinit var fundRepository: FundRepository
    private lateinit var snapshotRepository: PortfolioSnapshotRepository
    private lateinit var useCase: GetPortfolioUseCase

    @Before
    fun setUp() {
        holdingRepository = mock()
        fundRepository = mock()
        snapshotRepository = mock()
        runBlocking {
            whenever(snapshotRepository.getAllOnce()).thenReturn(emptyList())
        }
        useCase = GetPortfolioUseCase(
            holdingRepository = holdingRepository,
            stockRepository = mock<StockRepository>(),
            fundRepository = fundRepository,
            exchangeRateRepository = mock<ExchangeRateRepository>(),
            goldYestCloseCache = mock<GoldYestCloseCache>(),
            snapshotRepository = snapshotRepository,
            getTransactionSummaryUseCase = mock<GetTransactionSummaryUseCase>(),
            prefs = mock<SharedPreferences>(),
        )
    }

    @Test
    fun fundQuoteMissingFromRemote_usesCachedQuote() = runTest {
        whenever(holdingRepository.getActiveHoldings()).thenReturn(flowOf(listOf(fundHolding())))
        whenever(fundRepository.getFundQuotes(listOf(FUND_CODE))).thenReturn(emptyList())
        whenever(fundRepository.getCachedFundQuotes(listOf(FUND_CODE))).thenReturn(listOf(fundQuote(1.2)))

        val portfolio = useCase.getPortfolioWithQuotes()

        assertEquals(120.0, portfolio.totalMarketValue, 0.001)
        assertEquals(20.0, portfolio.totalEarnings, 0.001)
        assertEquals(1.2, portfolio.holdings.single().currentPrice, 0.001)
        verify(fundRepository).getCachedFundQuotes(listOf(FUND_CODE))
    }

    @Test
    fun fundQuoteUnavailableWithoutCache_usesCostInsteadOfZero() = runTest {
        whenever(holdingRepository.getActiveHoldings()).thenReturn(flowOf(listOf(fundHolding())))
        whenever(fundRepository.getFundQuotes(listOf(FUND_CODE))).thenReturn(emptyList())
        whenever(fundRepository.getCachedFundQuotes(listOf(FUND_CODE))).thenReturn(emptyList())

        val portfolio = useCase.getPortfolioWithQuotes()

        assertEquals(100.0, portfolio.totalMarketValue, 0.001)
        assertEquals(0.0, portfolio.totalEarnings, 0.001)
        assertEquals(1.0, portfolio.holdings.single().currentPrice, 0.001)
    }

    private fun fundHolding() = Holding(
        code = FUND_CODE,
        name = "测试基金",
        marketType = MarketType.FUND,
        costPrice = 1.0,
        holdingAmount = 100.0,
        holdingShares = 100.0,
    )

    private fun fundQuote(value: Double) = FundQuote(
        code = FUND_CODE,
        name = "测试基金",
        estimatedValue = value,
        dailyChangePercent = 0.0,
        netAssetValue = value,
        estimateTime = "10:00",
        navDate = "2026-07-20",
    )

    private companion object {
        const val FUND_CODE = "110011"
    }
}
