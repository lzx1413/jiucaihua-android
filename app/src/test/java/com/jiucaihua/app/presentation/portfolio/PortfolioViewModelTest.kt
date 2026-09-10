package com.jiucaihua.app.presentation.portfolio

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioPeriodReturnsUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import com.jiucaihua.app.domain.usecase.RecordSnapshotUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val portfolio = mock<GetPortfolioUseCase>()
    private val snapshots = mock<PortfolioSnapshotRepository>()
    private val periods = mock<GetPortfolioPeriodReturnsUseCase>()
    private val calendar = mock<IsMarketOpenUseCase>()
    private val recorder = mock<RecordSnapshotUseCase>()
    private val news = mock<NewsRepository>()
    private val prefs = mock<SharedPreferences>()
    private val holdings = MutableStateFlow(emptyList<Holding>())

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private suspend fun createViewModel(): PortfolioViewModel {
        whenever(portfolio.observeHoldings()).thenReturn(holdings)
        whenever(portfolio.getPortfolioFromCache()).thenReturn(PortfolioSummary())
        whenever(snapshots.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(periods(any(), any())).thenReturn(emptyList())
        whenever(calendar.getMarketSessions()).thenReturn(mapOf(MarketType.A_STOCK to MarketSession.TRADING))
        whenever(prefs.getInt("refresh_interval_seconds", 10)).thenReturn(10)
        return PortfolioViewModel(
            context = mock<Context>(), manageHoldingUseCase = mock(), getPortfolioUseCase = portfolio,
            getPortfolioPeriodReturnsUseCase = periods, getPortfolioReturnHistoryUseCase = mock(),
            getTransactionSummaryUseCase = mock(), recordSnapshotUseCase = recorder,
            isMarketOpenUseCase = calendar, newsRepository = news, snapshotRepository = snapshots,
            addTransactionUseCase = mock(), prefs = prefs, computationDispatcher = dispatcher,
        ).also { store.put("portfolio", it) }
    }

    @Test fun hiddenPageDoesNotFetchQuotesOrNews() = runTest(dispatcher) {
        createViewModel()
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        verify(portfolio, never()).getPortfolioWithQuotes()
        verifyNoInteractions(news)
    }

    @Test fun slowRefreshWaitsBeforeNextRoundAndStopsWhenHidden() = runTest(dispatcher) {
        val vm = createViewModel()
        var calls = 0
        whenever(portfolio.getPortfolioWithQuotes()).doSuspendableAnswer { calls++; delay(25_000); PortfolioSummary() }
        vm.setHoldingsVisible(true)
        runCurrent()
        advanceTimeBy(24_000)
        runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(11_000)
        runCurrent()
        assertEquals(2, calls)
        vm.setHoldingsVisible(false)
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(2, calls)
        assertFalse(vm.uiState.value.isRefreshing)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test fun cacheCannotOverwriteFreshQuotesAndSnapshotReusesSummary() = runTest(dispatcher) {
        val vm = createViewModel()
        val cache = CompletableDeferred<PortfolioSummary>()
        val fresh = PortfolioSummary(totalMarketValue = 200.0)
        whenever(portfolio.getPortfolioFromCache()).doSuspendableAnswer { cache.await() }
        whenever(portfolio.getPortfolioWithQuotes()).thenReturn(fresh)
        vm.setHoldingsVisible(true)
        runCurrent()
        cache.complete(PortfolioSummary(totalMarketValue = 100.0))
        runCurrent()
        assertEquals(200.0, vm.uiState.value.summary.totalMarketValue, 0.001)
        verify(recorder).recordSnapshot(fresh)
        vm.setHoldingsVisible(false)
    }

    @Test fun holdingsChangedDuringFetchInvalidateOldResponseAndFetchAgain() = runTest(dispatcher) {
        val vm = createViewModel()
        val response = CompletableDeferred<PortfolioSummary>()
        whenever(portfolio.getPortfolioWithQuotes()).doSuspendableAnswer { response.await() }
        vm.setHoldingsVisible(true)
        runCurrent()
        holdings.value = listOf(Holding(id = 1, code = "sh600000", name = "Test", marketType = MarketType.A_STOCK, costPrice = 1.0, holdingAmount = 1.0, holdingShares = 1.0))
        runCurrent()
        response.complete(PortfolioSummary(totalMarketValue = 200.0))
        runCurrent()
        verify(portfolio, times(2)).getPortfolioWithQuotes()
        // Only the second, current round may record a closing snapshot.
        verify(recorder, times(1)).recordSnapshot(any())
        assertEquals(200.0, vm.uiState.value.summary.totalMarketValue, 0.001)
        assertFalse(vm.uiState.value.isRefreshing)
        vm.setHoldingsVisible(false)
    }
}
