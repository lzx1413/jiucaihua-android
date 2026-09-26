package com.jiucaihua.app.cetp

import com.jiucaihua.app.ai.model.*
import com.jiucaihua.app.ai.tool.*
import com.jiucaihua.app.ai.usecase.*
import com.jiucaihua.app.domain.model.*
import com.jiucaihua.app.domain.repository.*
import com.jiucaihua.app.domain.usecase.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class CetpToolContractTest {
    @Test
    fun invalidParametersNeverReachRepositories() = runTest {
        val builder = mock<BuildKLineToolSnapshotUseCase>()
        val registry = ToolRegistry(setOf(GetKLineDataTool(builder)))
        val base = mapOf<String, Any?>("code" to "sh600519")
        val invalid = listOf(
            emptyMap(), base + ("limit" to "60"), base + ("limit" to 1.5),
            base + ("limit" to -1), base + ("limit" to 121), base + ("period" to "HOURLY"),
            base + ("include_latest" to "false"), base + ("typo" to true),
        )
        invalid.forEach { assertEquals(it.toString(), "INVALID_ARGS", registry.execute("get_kline_data", it).error?.code) }
        verifyNoInteractions(builder)
    }

    @Test
    fun invalidMutationDoesNotWriteAndMissingAlertIsNotFound() = runTest {
        val repo = mock<AlertRepository>()
        val registry = ToolRegistry(setOf(CreateAlertTool(repo), DeleteAlertTool(repo)))
        val args = mapOf("code" to "sh600519", "name" to "测试", "alertType" to "PRICE_ABOVE", "threshold" to -1)
        assertEquals("INVALID_ARGS", registry.execute("create_alert", args).error?.code)
        assertEquals("INVALID_ARGS", registry.execute("delete_alert", mapOf("id" to 1.5)).error?.code)
        verifyNoInteractions(repo)
        assertEquals("NOT_FOUND", registry.execute("delete_alert", mapOf("id" to 1)).error?.code)
        verify(repo, never()).deleteAlert(any())
    }

    @Test
    fun stockFlowPeriodControlsReturnedSeries() = runTest {
        val repo = mock<SecurityEventRepository>()
        val id = SecurityId.parse("sh600519")!!
        whenever(repo.getStockFundFlow(id)).thenReturn(StockFundFlowSnapshot(id,
            mainNet = 0.0,
            intradayPoints = listOf(StockFundFlowPoint("09:30", 1.0)),
            fiveDaySummary = listOf(StockFundFlowPoint("2026-09-25", 2.0)),
        ))
        val registry = ToolRegistry(setOf(GetStockFundFlowTool(repo)))
        for (period in listOf("summary", "intraday", "5d", "both")) {
            val result = registry.execute("get_stock_fund_flow", mapOf("code" to id.value, "period" to period))
            val json = JSONObject(CetpToolResultSerializer().toJson(result.content))
            assertEquals(period == "intraday" || period == "both", json.has("intradayPoints"))
            assertEquals(period == "5d" || period == "both", json.has("fiveDaySummary"))
            assertEquals(0.0, json.getDouble("mainNet"), 0.0)
            assertFalse(json.has("retailNet"))
        }
    }

    @Test
    fun stocksAndFundsUseTheirOwnSourcesAndMissingPriceIsOmitted() = runTest {
        val watchlist = mock<WatchlistRepository>()
        val stocks = mock<StockRepository>()
        val funds = mock<FundRepository>()
        whenever(watchlist.getAllWatchlist()).thenReturn(flowOf(listOf(
            WatchlistItem(code = "sh600519", name = "股票", marketType = MarketType.A_STOCK),
            WatchlistItem(code = "110011", name = "基金", marketType = MarketType.FUND),
            WatchlistItem(code = "110012", name = "缺失", marketType = MarketType.FUND),
        )))
        whenever(stocks.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(
            StockQuote("sh600519", "股票", 10.0, 9.0, 9.0, 10.0, 9.0, 0.0, 0.0, 11.1, 1.0, "2026-09-25 15:00:00", MarketType.A_STOCK)
        ))
        whenever(funds.getFundQuotes(listOf("110011"))).thenReturn(listOf(FundQuote("110011", "基金", 1.2, 2.0, 1.1, "2026-09-25 15:00", "2026-09-24")))
        whenever(funds.getFundQuotes(listOf("110012"))).thenReturn(listOf(FundQuote("110012", "缺失", 0.0, 0.0, 0.0, "", "")))
        val result = BuildWatchlistSnapshotUseCase(watchlist, stocks, funds)()
        assertEquals(10.0, result.items[0].currentPrice!!, 0.0)
        assertEquals(1.2, result.items[1].currentPrice!!, 0.0)
        assertNull(result.items[2].currentPrice)
        assertEquals("unavailable", result.items[2].quoteStatus)
        verify(funds, never()).getFundQuotes(listOf("sh600519"))
        val json = JSONObject(CetpToolResultSerializer().toJson(result))
        assertFalse(json.getJSONArray("items").getJSONObject(2).has("currentPrice"))
    }

    @Test
    fun requestedMarketDoesNotFetchOtherMarkets() = runTest {
        val repo = mock<MarketRepository>()
        whenever(repo.getHKStockIndices()).thenReturn(emptyList())
        BuildMarketIndicesSnapshotUseCase(repo)("HK_STOCK")
        verify(repo).getHKStockIndices()
        verifyNoMoreInteractions(repo)
    }

    @Test
    fun cachedPortfolioDoesNotClaimLiveOrUseGenerationTimeAsQuoteTime() = runTest {
        val portfolio = mock<GetPortfolioUseCase>()
        val alerts = mock<AlertRepository>()
        val market = mock<IsMarketOpenUseCase>()
        val holding = Holding(code = "sh600519", name = "股票", marketType = MarketType.A_STOCK, costPrice = 9.0, holdingAmount = 0.0, holdingShares = 100.0, currentPrice = 10.0)
        whenever(portfolio.getPortfolioWithQuotes()).thenReturn(PortfolioSummary(holdings = listOf(holding), lastUpdateTime = "16:00:00", quoteObservations = mapOf(holding.code to QuoteObservation("2026-09-25 15:00:00", true, true))))
        whenever(alerts.getEnabledAlerts()).thenReturn(emptyList())
        whenever(market.getMarketSessions()).thenReturn(emptyMap())
        val result = BuildPortfolioAnalysisSnapshotUseCase(portfolio, alerts, market)()
        assertEquals(DataSource.CACHE, result.dataFreshness.source)
        assertNull(result.dataFreshness.quoteDisplayTime)
        assertEquals("2026-09-25 15:00:00", result.holdings.single().latestQuoteTime)
        assertEquals(DataSource.CACHE, result.holdings.single().dataFreshness.source)
    }

    @Test
    fun missingIndicatorDataDoesNotBecomeZeroPrice() = runTest {
        val stocks = mock<StockRepository>()
        whenever(stocks.getKLineData("sh600519", KLinePeriod.DAILY, 125)).thenReturn(KLineData("sh600519", "股票", KLinePeriod.DAILY, emptyList()))
        val result = BuildIndicatorSnapshotUseCase(GetKLineDataUseCase(stocks), mock())("sh600519", null, null)
        assertNull(result.price)
        assertEquals("unavailable", result.status)
    }

    @Test
    fun contextOmitsUnrequestedSectionsAndAvoidsThoseFetches() = runTest {
        val stocks = mock<StockRepository>()
        val events = mock<SecurityEventRepository>()
        val insights = mock<SecurityInsightRepository>()
        whenever(stocks.getAStockQuotes(listOf("sh600519"))).thenReturn(emptyList())
        whenever(stocks.getKLineData("sh600519", KLinePeriod.DAILY, 60)).thenReturn(KLineData("sh600519", "", KLinePeriod.DAILY, emptyList()))
        val result = GetStockContextTool(stocks, events, insights).execute(mapOf("code" to "sh600519"))
        val json = JSONObject(CetpToolResultSerializer().toJson(result.content))
        assertTrue(json.has("quote"))
        assertFalse(json.has("events"))
        assertFalse(json.has("stockFundFlow"))
        assertFalse(json.has("profile"))
        verifyNoInteractions(events, insights)
    }
    @Test
    fun stockNewsReturnsOneArticleArrayAndRejectsInvalidCodeEvenWithName() = runTest {
        val news = mock<NewsRepository>()
        val events = mock<SecurityEventRepository>()
        val registry = ToolRegistry(setOf(GetStockNewsTool(news, events)))
        assertEquals("INVALID_ARGS", registry.execute("get_stock_news", mapOf("code" to "bad", "name" to "股票")).error?.code)
        verifyNoInteractions(news, events)
        val code = SecurityId.parse("sh600519")!!
        whenever(events.getEvents(code, GetStockEventsTool.SUPPORTED_KINDS, 10)).thenReturn(listOf(
            SecurityEvent("id", kind = SecurityEventKind.NEWS, title = "测试", publishedAt = 1_000L, symbols = listOf(code))
        ))
        val result = registry.execute("get_stock_news", mapOf("code" to code.value))
        val json = JSONObject(CetpToolResultSerializer().toJson(result.content))
        assertEquals(1, json.getJSONArray("articles").length())
        assertFalse(json.has("events"))
        assertEquals("1970-01-01T00:00:01Z", json.getJSONArray("articles").getJSONObject(0).getString("time"))
    }

    @Test
    fun reversedTimeRangeFailsBeforeFetchingTransactions() = runTest {
        val builder = mock<BuildTransactionsToolSnapshotUseCase>()
        val registry = ToolRegistry(setOf(GetTransactionsTool(builder)))
        assertEquals("INVALID_ARGS", registry.execute("get_transactions", mapOf("from" to 2_000L, "to" to 1_000L)).error?.code)
        verifyNoInteractions(builder)
    }

    @Test
    fun transactionPageExposesWhetherMoreRowsExist() = runTest {
        val repository = mock<TransactionRepository>()
        val query = TransactionQuery(limit = 1, offset = 1)
        whenever(repository.count(query)).thenReturn(3)
        whenever(repository.query(query)).thenReturn(listOf(InvestmentTransaction(type = TransactionType.BUY, tradeDate = 1_000L)))
        val result = BuildTransactionsToolSnapshotUseCase(GetTransactionsUseCase(repository))(query)
        assertEquals(3, result.total)
        assertEquals(1, result.offset)
        assertTrue(result.hasMore)
        whenever(repository.count(query)).thenReturn(2)
        assertFalse(BuildTransactionsToolSnapshotUseCase(GetTransactionsUseCase(repository))(query).hasMore)
    }

}
