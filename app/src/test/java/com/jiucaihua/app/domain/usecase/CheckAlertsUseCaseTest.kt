package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.StockRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckAlertsUseCaseTest {

    private lateinit var alertRepository: AlertRepository
    private lateinit var stockRepository: StockRepository
    private lateinit var fundRepository: FundRepository
    private lateinit var marketCalendarRepository: MarketCalendarRepository
    private lateinit var useCase: CheckAlertsUseCase

    private val aStockQuote = StockQuote(
        code = "sh600519", name = "贵州茅台", price = 1750.0, yestClose = 1700.0,
        open = 1710.0, high = 1760.0, low = 1705.0, volume = 10000.0,
        amount = 17500000.0, changePercent = 2.94, changeAmount = 50.0,
        time = "10:30:00", marketType = MarketType.A_STOCK,
    )

    private val hkStockQuote = StockQuote(
        code = "hk00700", name = "腾讯", price = 420.0, yestClose = 415.0,
        open = 416.0, high = 425.0, low = 413.0, volume = 5000.0,
        amount = 2100000.0, changePercent = 1.2, changeAmount = 5.0,
        time = "10:30:00", marketType = MarketType.HK_STOCK,
    )

    private val usStockQuote = StockQuote(
        code = "gb_AAPL", name = "苹果", price = 190.0, yestClose = 188.0,
        open = 189.0, high = 192.0, low = 187.0, volume = 8000.0,
        amount = 1520000.0, changePercent = 1.06, changeAmount = 2.0,
        time = "10:30:00", marketType = MarketType.US_STOCK,
    )

    private val fundQuote = FundQuote(
        code = "110011", name = "易方达中小盘", estimatedValue = 5.5,
        dailyChangePercent = 1.8, netAssetValue = 5.3, estimateTime = "10:30",
        navDate = "2026-05-20",
    )

    @Before
    fun setup() {
        alertRepository = mock()
        stockRepository = mock()
        fundRepository = mock()
        marketCalendarRepository = mock()
        useCase = CheckAlertsUseCase(alertRepository, stockRepository, fundRepository, marketCalendarRepository)
    }

    private suspend fun givenAllMarketsTrading() {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.TRADING,
            MarketType.HK_STOCK to MarketSession.TRADING,
            MarketType.US_STOCK to MarketSession.TRADING,
            MarketType.GOLD to MarketSession.TRADING,
        ))
    }

    private suspend fun givenNoMarketsTrading() {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.CLOSED,
            MarketType.HK_STOCK to MarketSession.CLOSED,
            MarketType.US_STOCK to MarketSession.CLOSED,
            MarketType.GOLD to MarketSession.CLOSED,
        ))
    }

    private suspend fun givenOnlyUSTrading() {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.CLOSED,
            MarketType.HK_STOCK to MarketSession.CLOSED,
            MarketType.US_STOCK to MarketSession.TRADING,
            MarketType.GOLD to MarketSession.CLOSED,
        ))
    }

    // --- Basic trigger logic ---

    @Test
    fun noAlerts_returnsEmptyList() = runTest {
        whenever(alertRepository.getEnabledAlerts()).thenReturn(emptyList())

        val result = useCase.checkAlerts()

        assertEquals(emptyList<TriggeredAlert>(), result)
    }

    @Test
    fun priceAboveAlert_triggeredWhenPriceExceedsThreshold() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(1750.0, result[0].currentValue, 0.01)
        assertEquals(alert.id, result[0].alert.id)
        verify(alertRepository).markTriggered(alert.id)
    }

    @Test
    fun priceAboveAlert_notTriggeredWhenPriceBelowThreshold() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1800.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    @Test
    fun priceBelowAlert_triggeredWhenPriceDropsBelowThreshold() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 2, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_BELOW, threshold = 1800.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(1750.0, result[0].currentValue, 0.01)
    }

    // --- Correct currentValue for change alerts (key bug fix) ---

    @Test
    fun changeAboveAlert_passesChangePercentNotPrice() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 3, code = "sh600519", name = "贵州茅台", alertType = AlertType.CHANGE_ABOVE, threshold = 2.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(2.94, result[0].currentValue, 0.01)
    }

    @Test
    fun changeBelowAlert_passesChangePercentNotPrice() = runTest {
        givenAllMarketsTrading()
        val droppingQuote = aStockQuote.copy(price = 1650.0, changePercent = -2.94)
        val alert = PriceAlert(id = 4, code = "sh600519", name = "贵州茅台", alertType = AlertType.CHANGE_BELOW, threshold = 2.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(droppingQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(-2.94, result[0].currentValue, 0.01)
    }

    @Test
    fun changeBelowAlert_triggeredWhenDropExceedsThreshold() = runTest {
        givenAllMarketsTrading()
        val droppingQuote = aStockQuote.copy(price = 1650.0, changePercent = -2.5)
        val alert = PriceAlert(id = 4, code = "sh600519", name = "贵州茅台", alertType = AlertType.CHANGE_BELOW, threshold = 2.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(droppingQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
    }

    @Test
    fun changeBelowAlert_notTriggeredWhenDropWithinThreshold() = runTest {
        givenAllMarketsTrading()
        val slightDropQuote = aStockQuote.copy(price = 1680.0, changePercent = -1.5)
        val alert = PriceAlert(id = 4, code = "sh600519", name = "贵州茅台", alertType = AlertType.CHANGE_BELOW, threshold = 3.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(slightDropQuote))

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    // --- Cooldown ---

    @Test
    fun cooldownPreventsDuplicateTrigger() = runTest {
        givenAllMarketsTrading()
        val recentTime = System.currentTimeMillis() - 10 * 60 * 1000L
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0, lastTriggeredAt = recentTime)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    @Test
    fun alertTriggersAfterCooldownExpires() = runTest {
        givenAllMarketsTrading()
        val oldTime = System.currentTimeMillis() - 31 * 60 * 1000L
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0, lastTriggeredAt = oldTime)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
    }

    // --- Per-market trading session check ---

    @Test
    fun noMarketTrading_skipsAllStockAlerts() = runTest {
        givenNoMarketsTrading()
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    @Test
    fun onlyUSTrading_onlyChecksUSAlerts() = runTest {
        givenOnlyUSTrading()
        val aStockAlert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        val usStockAlert = PriceAlert(id = 2, code = "gb_AAPL", name = "苹果", alertType = AlertType.PRICE_ABOVE, threshold = 180.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(aStockAlert, usStockAlert))
        whenever(stockRepository.getUSStockQuotes(listOf("gb_AAPL"))).thenReturn(listOf(usStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals("gb_AAPL", result[0].alert.code)
        assertEquals(190.0, result[0].currentValue, 0.01)
    }

    @Test
    fun fundAlert_skippedWhenAStockNotTrading() = runTest {
        givenOnlyUSTrading()
        val fundAlert = PriceAlert(id = 1, code = "110011", name = "易方达中小盘", alertType = AlertType.PRICE_ABOVE, threshold = 5.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(fundAlert))

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    @Test
    fun fundAlert_triggeredWhenAStockTrading() = runTest {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.TRADING,
            MarketType.HK_STOCK to MarketSession.CLOSED,
            MarketType.US_STOCK to MarketSession.CLOSED,
            MarketType.GOLD to MarketSession.CLOSED,
        ))
        val fundAlert = PriceAlert(id = 1, code = "110011", name = "易方达中小盘", alertType = AlertType.PRICE_ABOVE, threshold = 5.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(fundAlert))
        whenever(fundRepository.getFundQuotes(listOf("110011"))).thenReturn(listOf(fundQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(5.5, result[0].currentValue, 0.01)
    }

    @Test
    fun fundChangeAlert_passesChangePercentAsCurrentValue() = runTest {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.TRADING,
            MarketType.HK_STOCK to MarketSession.CLOSED,
            MarketType.US_STOCK to MarketSession.CLOSED,
            MarketType.GOLD to MarketSession.CLOSED,
        ))
        val fundAlert = PriceAlert(id = 1, code = "110011", name = "易方达中小盘", alertType = AlertType.CHANGE_ABOVE, threshold = 1.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(fundAlert))
        whenever(fundRepository.getFundQuotes(listOf("110011"))).thenReturn(listOf(fundQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(1.8, result[0].currentValue, 0.01)
    }

    @Test
    fun hkStockAlert_checksHKMarket() = runTest {
        whenever(marketCalendarRepository.getMarketSessions()).thenReturn(mapOf(
            MarketType.A_STOCK to MarketSession.CLOSED,
            MarketType.HK_STOCK to MarketSession.TRADING,
            MarketType.US_STOCK to MarketSession.CLOSED,
            MarketType.GOLD to MarketSession.CLOSED,
        ))
        val hkAlert = PriceAlert(id = 1, code = "hk00700", name = "腾讯", alertType = AlertType.PRICE_ABOVE, threshold = 400.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(hkAlert))
        whenever(stockRepository.getHKStockQuotes(listOf("hk00700"))).thenReturn(listOf(hkStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(420.0, result[0].currentValue, 0.01)
    }

    // --- Error handling ---

    @Test
    fun apiFailure_fallsBackToCachedQuotes() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(any())).thenThrow(RuntimeException("Network error"))
        whenever(stockRepository.getCachedQuotes(listOf("sh600519"))).thenReturn(listOf(aStockQuote))

        val result = useCase.checkAlerts()

        assertEquals(1, result.size)
        assertEquals(1750.0, result[0].currentValue, 0.01)
    }

    @Test
    fun missingQuote_skipsAlert() = runTest {
        givenAllMarketsTrading()
        val alert = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519"))).thenReturn(emptyList())

        val result = useCase.checkAlerts()

        assertEquals(0, result.size)
    }

    @Test
    fun multipleAlerts_triggersCorrectOnes() = runTest {
        givenAllMarketsTrading()
        val alert1 = PriceAlert(id = 1, code = "sh600519", name = "贵州茅台", alertType = AlertType.PRICE_ABOVE, threshold = 1700.0)
        val alert2 = PriceAlert(id = 2, code = "sh600036", name = "招商银行", alertType = AlertType.PRICE_BELOW, threshold = 40.0)
        val bankQuote = StockQuote(
            code = "sh600036", name = "招商银行", price = 38.0, yestClose = 39.0,
            open = 39.0, high = 39.5, low = 37.5, volume = 20000.0,
            amount = 780000.0, changePercent = -2.56, changeAmount = -1.0,
            time = "10:30:00", marketType = MarketType.A_STOCK,
        )
        whenever(alertRepository.getEnabledAlerts()).thenReturn(listOf(alert1, alert2))
        whenever(stockRepository.getAStockQuotes(listOf("sh600519", "sh600036"))).thenReturn(listOf(aStockQuote, bankQuote))

        val result = useCase.checkAlerts()

        assertEquals(2, result.size)
    }
}
