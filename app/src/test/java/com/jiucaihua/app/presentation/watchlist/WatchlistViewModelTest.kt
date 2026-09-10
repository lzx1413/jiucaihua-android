package com.jiucaihua.app.presentation.watchlist

import androidx.lifecycle.ViewModelStore
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.WatchlistItem
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val stocks = mock<StockRepository>()
    private val watchlist = mock<WatchlistRepository>()
    private val calendar = mock<IsMarketOpenUseCase>()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun fetchesOneBatchPerMarketOnlyWhileVisible() = runTest(dispatcher) {
        whenever(watchlist.getAllWatchlist()).thenReturn(flowOf(listOf(
            WatchlistItem(id = 1, code = "sh600000", name = "A", marketType = MarketType.A_STOCK),
            WatchlistItem(id = 2, code = "sz000001", name = "B", marketType = MarketType.A_STOCK),
            WatchlistItem(id = 3, code = "hk00700", name = "C", marketType = MarketType.HK_STOCK),
        )))
        whenever(watchlist.observeGroups()).thenReturn(flowOf(emptyList()))
        whenever(stocks.getAStockQuotes(any())).thenReturn(emptyList())
        whenever(stocks.getHKStockQuotes(any())).thenReturn(emptyList())
        whenever(calendar.getMarketSessions()).thenReturn(mapOf(MarketType.A_STOCK to MarketSession.TRADING))
        val vm = WatchlistViewModel(watchlist, stocks, mock(), mock(), calendar, dispatcher)
        store.put("watchlist", vm)
        runCurrent()
        verifyNoInteractions(stocks)
        vm.setVisible(true)
        runCurrent()
        verify(stocks).getAStockQuotes(listOf("sh600000", "sz000001"))
        verify(stocks).getHKStockQuotes(listOf("hk00700"))
        vm.setVisible(false)
        advanceTimeBy(60_000)
        runCurrent()
        verify(stocks, times(1)).getAStockQuotes(any())
        verify(stocks, times(1)).getHKStockQuotes(any())
    }
}
