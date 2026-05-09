package com.jiucaihua.app.domain.usecase

import android.content.SharedPreferences
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
        val fundCodes = holdings.filter { it.marketType == MarketType.FUND }.map { it.code }
        val goldCodes = holdings.filter { it.marketType == MarketType.GOLD }.map { it.code }

        val (aQuotes, hkQuotes, fundQuotes, goldQuotes, hkdRate) = coroutineScope {
            val aDeferred = async { fetchAStockQuotes(aStockCodes) }
            val hkDeferred = async { fetchHKStockQuotes(hkStockCodes) }
            val fundDeferred = async { fetchFundQuotes(fundCodes) }
            val goldDeferred = async { fetchGoldQuotes(goldCodes) }
            val rateDeferred = async {
                if (hkStockCodes.isNotEmpty()) exchangeRateRepository.getHkdToCnyRate() else 1.0
            }
            QuintResult(aDeferred.await(), hkDeferred.await(), fundDeferred.await(), goldDeferred.await(), rateDeferred.await())
        }

        val stockQuotes = (aQuotes + hkQuotes + goldQuotes).associateBy { it.code }
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
                MarketType.US_STOCK -> holding
                MarketType.FUND -> {
                    val fq = fundQuoteMap[holding.code]
                    if (fq != null) {
                        holding.copy(
                            currentPrice = fq.estimatedValue,
                            changePercent = fq.dailyChangePercent,
                        )
                    } else holding
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
        val fundCodes = holdings.filter { it.marketType == MarketType.FUND }.map { it.code }
        val goldCodes = holdings.filter { it.marketType == MarketType.GOLD }.map { it.code }

        val allStockCodes = aStockCodes + hkStockCodes + goldCodes
        val stockQuotes = stockRepository.getCachedQuotes(allStockCodes).associateBy { it.code }
        val fundQuoteMap = fundRepository.getCachedFundQuotes(fundCodes).associateBy { it.code }

        val hkdRate = if (hkStockCodes.isNotEmpty()) {
            try { exchangeRateRepository.getHkdToCnyRate() } catch (_: Exception) { 0.92 }
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
                MarketType.US_STOCK -> holding
                MarketType.FUND -> {
                    val fq = fundQuoteMap[holding.code]
                    if (fq != null) {
                        holding.copy(
                            currentPrice = fq.estimatedValue,
                            changePercent = fq.dailyChangePercent,
                        )
                    } else holding
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
            fundRepository.getFundQuotes(codes)
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
        return if (holding.marketType == MarketType.FUND) {
            holding.holdingAmount * holding.exchangeRate
        } else {
            holding.costPrice * holding.holdingShares * holding.exchangeRate
        }
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
            MarketType.US_STOCK -> 0.0
            MarketType.FUND -> {
                if (fundQuote == null) return 0.0
                val dailyChangeRatio = fundQuote.dailyChangePercent / 100
                holding.marketValueCNY * dailyChangeRatio
            }
            MarketType.GOLD -> {
                if (stockQuote == null) return 0.0
                (stockQuote.price - stockQuote.yestClose) * holding.holdingShares * holding.exchangeRate
            }
        }
    }

    private data class QuintResult<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    companion object {
        private const val KEY_CASH = "cash"
        private const val KEY_LOSS_COMPENSATION = "loss_compensation"
    }
}
