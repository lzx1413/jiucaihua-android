package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
import com.jiucaihua.app.data.cache.GoldYestCloseCache
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class GetPortfolioUseCase @Inject constructor(
    private val holdingRepository: HoldingRepository,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val goldYestCloseCache: GoldYestCloseCache,
    @Named("appPrefs") private val prefs: SharedPreferences,
) {
    fun observeHoldings(): Flow<List<Holding>> {
        return holdingRepository.getActiveHoldings()
    }

    suspend fun getPortfolioWithQuotes(): PortfolioSummary {
        val holdings = holdingRepository.getActiveHoldings().first()
        if (holdings.isEmpty()) return PortfolioSummary()

        val aStockCodes = holdings.filter { it.marketType == MarketType.A_STOCK }.map { it.code }
        val hkStockCodes = holdings.filter { it.marketType == MarketType.HK_STOCK }.map { it.code }
        val usStockCodes = holdings.filter { it.marketType == MarketType.US_STOCK }.map { it.code }
        val fundCodes = holdings.filter { it.marketType == MarketType.FUND }.map { it.code }
        val goldCodes = holdings.filter { it.marketType == MarketType.GOLD }.map { it.code }

        val (aQuotes, hkQuotes, usQuotes, fundQuotes, goldQuotes, hkdRate, usdRate) = coroutineScope {
            val aDeferred = async { fetchAStockQuotes(aStockCodes) }
            val hkDeferred = async { fetchHKStockQuotes(hkStockCodes) }
            val usDeferred = async { fetchUSStockQuotes(usStockCodes) }
            val fundDeferred = async { fetchFundQuotes(fundCodes) }
            val goldDeferred = async { fetchGoldQuotes(goldCodes) }
            val hkdRateDeferred = async {
                if (hkStockCodes.isNotEmpty()) try { exchangeRateRepository.getHkdToCnyRate() } catch (_: Exception) { DEFAULT_HKD_RATE } else 1.0
            }
            val usdRateDeferred = async {
                if (usStockCodes.isNotEmpty()) try { exchangeRateRepository.getUsdToCnyRate() } catch (_: Exception) { DEFAULT_USD_RATE } else 1.0
            }
            SeptResult(aDeferred.await(), hkDeferred.await(), usDeferred.await(), fundDeferred.await(), goldDeferred.await(), hkdRateDeferred.await(), usdRateDeferred.await())
        }

        val stockQuotes = (aQuotes + hkQuotes + usQuotes + correctGoldQuotes(goldQuotes)).associateBy { it.code }
        val fundQuoteMap = fundQuotes.associateBy { it.code }

        val updatedHoldings = holdings.map { holding ->
            when (holding.marketType) {
                MarketType.A_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(currentPrice = quote.price, changePercent = quote.changePercent)
                    } else holding
                }
                MarketType.HK_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                            exchangeRate = hkdRate,
                        )
                    } else holding.copy(exchangeRate = hkdRate)
                }
                MarketType.US_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                            exchangeRate = usdRate,
                        )
                    } else holding.copy(exchangeRate = usdRate)
                }
                MarketType.FUND -> {
                    val fq = fundQuoteMap[holding.code]
                    val price = fq?.effectiveValue()
                    if (price != null) {
                        holding.copy(
                            currentPrice = price,
                            changePercent = fq.dailyChangePercent,
                        )
                    } else holding.withFundPriceFallback()
                }
                MarketType.GOLD -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(currentPrice = quote.price, changePercent = quote.changePercent)
                    } else holding
                }
            }
        }

        return buildSummary(updatedHoldings, stockQuotes, fundQuoteMap)
    }

    suspend fun getPortfolioFromCache(): PortfolioSummary {
        val holdings = holdingRepository.getActiveHoldings().first()
        if (holdings.isEmpty()) return PortfolioSummary()

        val aStockCodes = holdings.filter { it.marketType == MarketType.A_STOCK }.map { it.code }
        val hkStockCodes = holdings.filter { it.marketType == MarketType.HK_STOCK }.map { it.code }
        val usStockCodes = holdings.filter { it.marketType == MarketType.US_STOCK }.map { it.code }
        val fundCodes = holdings.filter { it.marketType == MarketType.FUND }.map { it.code }
        val goldCodes = holdings.filter { it.marketType == MarketType.GOLD }.map { it.code }

        val allStockCodes = aStockCodes + hkStockCodes + usStockCodes + goldCodes
        val cachedStockQuotes = stockRepository.getCachedQuotes(allStockCodes)
        val goldCachedQuotes = cachedStockQuotes.filter { it.marketType == MarketType.GOLD }
        val nonGoldCachedQuotes = cachedStockQuotes.filter { it.marketType != MarketType.GOLD }
        val stockQuotes = (nonGoldCachedQuotes + correctGoldQuotes(goldCachedQuotes)).associateBy { it.code }
        val fundQuoteMap = fundRepository.getCachedFundQuotes(fundCodes).associateBy { it.code }

        val hkdRate = if (hkStockCodes.isNotEmpty()) {
            try { exchangeRateRepository.getHkdToCnyRate() } catch (_: Exception) { DEFAULT_HKD_RATE }
        } else 1.0

        val usdRate = if (usStockCodes.isNotEmpty()) {
            try { exchangeRateRepository.getUsdToCnyRate() } catch (_: Exception) { DEFAULT_USD_RATE }
        } else 1.0

        val updatedHoldings = holdings.map { holding ->
            when (holding.marketType) {
                MarketType.A_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(currentPrice = quote.price, changePercent = quote.changePercent)
                    } else holding
                }
                MarketType.HK_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                            exchangeRate = hkdRate,
                        )
                    } else holding.copy(exchangeRate = hkdRate)
                }
                MarketType.US_STOCK -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(
                            currentPrice = quote.price,
                            changePercent = quote.changePercent,
                            exchangeRate = usdRate,
                        )
                    } else holding.copy(exchangeRate = usdRate)
                }
                MarketType.FUND -> {
                    val fq = fundQuoteMap[holding.code]
                    val price = fq?.effectiveValue()
                    if (price != null) {
                        holding.copy(
                            currentPrice = price,
                            changePercent = fq.dailyChangePercent,
                        )
                    } else holding.withFundPriceFallback()
                }
                MarketType.GOLD -> {
                    val quote = stockQuotes[holding.code]
                    if (quote != null) {
                        holding.copy(currentPrice = quote.price, changePercent = quote.changePercent)
                    } else holding
                }
            }
        }

        return buildSummary(updatedHoldings, stockQuotes, fundQuoteMap)
    }

    private suspend fun fetchAStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()
        return try {
            stockRepository.getAStockQuotes(codes)
        } catch (_: Exception) {
            stockRepository.getCachedQuotes(codes)
        }
    }

    private suspend fun fetchHKStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()
        return try {
            stockRepository.getHKStockQuotes(codes)
        } catch (_: Exception) {
            stockRepository.getCachedQuotes(codes)
        }
    }

    private suspend fun fetchFundQuotes(codes: List<String>): List<FundQuote> {
        if (codes.isEmpty()) return emptyList()
        return try {
            val remoteQuotes = fundRepository.getFundQuotes(codes).filter { it.effectiveValue() != null }
            val remoteCodes = remoteQuotes.mapTo(mutableSetOf()) { it.code }
            val cachedQuotes = fundRepository.getCachedFundQuotes(codes.filterNot { it in remoteCodes })
            remoteQuotes + cachedQuotes.filter { it.effectiveValue() != null }
        } catch (_: Exception) {
            fundRepository.getCachedFundQuotes(codes)
        }
    }

    private suspend fun fetchGoldQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()
        return try {
            stockRepository.getGoldQuotes(codes)
        } catch (_: Exception) {
            stockRepository.getCachedQuotes(codes)
        }
    }

    private suspend fun fetchUSStockQuotes(codes: List<String>): List<StockQuote> {
        if (codes.isEmpty()) return emptyList()
        return try {
            stockRepository.getUSStockQuotes(codes)
        } catch (_: Exception) {
            stockRepository.getCachedQuotes(codes)
        }
    }

    private fun buildSummary(
        holdings: List<Holding>,
        stockQuotes: Map<String, StockQuote>,
        fundQuotes: Map<String, FundQuote>,
    ): PortfolioSummary {
        if (holdings.isEmpty()) return PortfolioSummary()

        val totalMarketValue = holdings.sumOf { it.marketValueCNY }
        val totalCost = holdings.sumOf { calcCostCNY(it) }
        val totalEarnings = totalMarketValue - totalCost
        val todayEarnings = holdings.sumOf { calcTodayEarnings(it, stockQuotes[it.code], fundQuotes[it.code]) }

        val cash = prefs.getFloat(KEY_CASH, 0f).toDouble()
        val lossCompensation = prefs.getFloat(KEY_LOSS_COMPENSATION, 0f).toDouble()
        val totalInvestment = totalCost + cash
        val totalEarningsPercent = if (totalInvestment > 0) totalEarnings / totalInvestment * 100 else 0.0
        val cumulativeEarnings = totalEarnings - lossCompensation
        val cumulativeEarningsPercent = if (totalInvestment > 0) cumulativeEarnings / totalInvestment * 100 else 0.0

        val categorySummaries = buildCategorySummaries(holdings, stockQuotes, fundQuotes)

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        return PortfolioSummary(
            cash = cash,
            lossCompensation = lossCompensation,
            totalMarketValue = totalMarketValue,
            totalCost = totalCost,
            totalInvestment = totalInvestment,
            totalEarnings = totalEarnings,
            totalEarningsPercent = totalEarningsPercent,
            cumulativeEarnings = cumulativeEarnings,
            cumulativeEarningsPercent = cumulativeEarningsPercent,
            todayEarnings = todayEarnings,
            holdings = holdings,
            categorySummaries = categorySummaries,
            lastUpdateTime = timeFormat.format(Date()),
        )
    }

    private fun buildCategorySummaries(
        holdings: List<Holding>,
        stockQuotes: Map<String, StockQuote>,
        fundQuotes: Map<String, FundQuote>,
    ): List<CategorySummary> {
        return MarketType.entries.mapNotNull { marketType ->
            val typeHoldings = holdings.filter { it.marketType == marketType }
            if (typeHoldings.isEmpty()) return@mapNotNull null

            val marketValue = typeHoldings.sumOf { it.marketValueCNY }
            val cost = typeHoldings.sumOf { calcCostCNY(it) }
            val earnings = marketValue - cost
            val earningsPercent = if (cost > 0) earnings / cost * 100 else 0.0
            val todayEarnings = typeHoldings.sumOf {
                calcTodayEarnings(it, stockQuotes[it.code], fundQuotes[it.code])
            }

            CategorySummary(
                marketType = marketType,
                totalMarketValue = marketValue,
                totalCost = cost,
                totalEarnings = earnings,
                totalEarningsPercent = earningsPercent,
                todayEarnings = todayEarnings,
                holdings = typeHoldings,
            )
        }
    }

    private fun calcCostCNY(holding: Holding): Double {
        return holding.costPrice * holding.holdingShares * holding.exchangeRate
    }

    private fun FundQuote.effectiveValue(): Double? {
        return estimatedValue.takeIf { it > 0 } ?: netAssetValue.takeIf { it > 0 }
    }

    private fun Holding.withFundPriceFallback(): Holding {
        // A new holding has no in-memory quote. Until a valid quote is available,
        // show it at cost instead of displaying a fictitious 100% loss.
        val fallbackPrice = currentPrice.takeIf { it > 0 } ?: costPrice
        return copy(currentPrice = fallbackPrice, changePercent = 0.0)
    }

    private fun calcTodayEarnings(holding: Holding, stockQuote: StockQuote?, fundQuote: FundQuote?): Double {
        if (holding.currentPrice <= 0) return 0.0

        return when (holding.marketType) {
            MarketType.A_STOCK -> {
                if (stockQuote == null) return 0.0
                (stockQuote.price - stockQuote.yestClose) * holding.holdingShares * holding.exchangeRate
            }
            MarketType.HK_STOCK -> {
                if (stockQuote == null) return 0.0
                (stockQuote.price - stockQuote.yestClose) * holding.holdingShares * holding.exchangeRate
            }
            MarketType.US_STOCK -> {
                if (stockQuote == null) return 0.0
                (stockQuote.price - stockQuote.yestClose) * holding.holdingShares * holding.exchangeRate
            }
            MarketType.FUND -> {
                if (fundQuote == null) return 0.0
                val dailyChangeRatio = fundQuote.dailyChangePercent / 100
                holding.marketValueCNY * dailyChangeRatio
            }
            MarketType.GOLD -> {
                if (stockQuote == null) return 0.0
                // 使用 correctedGoldQuotes 中已修正的 yestClose（日盘基准）
                (stockQuote.price - stockQuote.yestClose) * holding.holdingShares * holding.exchangeRate
            }
        }
    }

    /**
     * 修正黄金行情的昨收价。
     *
     * 上海金交所品种在日盘返回的 yestClose 是前一夜盘收盘价，
     * 在夜盘返回的 yestClose 是当日日盘收盘价，导致今日收益基准不一致。
     *
     * 修正逻辑：
     * - 首次获取今日行情时，缓存API返回的 yestClose（此时是日盘，值为前一夜盘收盘价）
     * - 后续获取时（夜盘），使用缓存的 yestClose 替代API返回的值，重新计算涨跌幅
     * - 如果首次获取发生在夜盘时段，则使用API的 yestClose 作为基准（降级处理）
     */
    private fun correctGoldQuotes(goldQuotes: List<StockQuote>): List<StockQuote> {
        return goldQuotes.map { quote ->
            val cachedYestClose = goldYestCloseCache.getYestClose(quote.code)

            if (cachedYestClose != null && cachedYestClose > 0 && cachedYestClose != quote.yestClose) {
                // 夜盘时段：使用缓存的日盘昨收价作为基准，重新计算涨跌
                val correctedYestClose = cachedYestClose
                val correctedChangeAmount = quote.price - correctedYestClose
                val correctedChangePercent = if (correctedYestClose > 0) {
                    correctedChangeAmount / correctedYestClose * 100
                } else 0.0

                quote.copy(
                    yestClose = correctedYestClose,
                    changeAmount = correctedChangeAmount,
                    changePercent = correctedChangePercent,
                )
            } else {
                // 日盘时段或首次获取：缓存当前的 yestClose
                if (quote.yestClose > 0) {
                    goldYestCloseCache.cacheYestClose(quote.code, quote.yestClose)
                }
                quote
            }
        }
    }

    private data class SeptResult<A, B, C, D, E, F, G>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F, val seventh: G)

    companion object {
        private const val KEY_CASH = "cash"
        private const val KEY_LOSS_COMPENSATION = "loss_compensation"
        private const val DEFAULT_HKD_RATE = 0.92
        private const val DEFAULT_USD_RATE = 7.2
    }
}
