package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.WatchlistItemSnapshot
import com.jiucaihua.app.ai.model.WatchlistSnapshot
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class BuildWatchlistSnapshotUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val stockRepository: StockRepository,
    private val fundRepository: FundRepository,
) {
    suspend operator fun invoke(): WatchlistSnapshot {
        val items = watchlistRepository.getAllWatchlist().first()
        val snapshots = items.map { item ->
            val missing = WatchlistItemSnapshot(item.code, item.name, item.marketType.name, quoteStatus = "unavailable")
            try {
                if (item.marketType == MarketType.FUND) {
                    val quote = fundRepository.getFundQuotes(listOf(item.code)).firstOrNull { it.code == item.code }
                    val estimate = quote?.estimatedValue?.takeIf { it.isFinite() && it > 0 }
                    val nav = quote?.netAssetValue?.takeIf { it.isFinite() && it > 0 }
                    val price = estimate ?: nav
                    if (quote == null || price == null) missing else missing.copy(
                        currentPrice = price,
                        changePercent = quote.dailyChangePercent.takeIf { it.isFinite() },
                        changeAmount = if (estimate != null && nav != null) estimate - nav else null,
                        quoteStatus = if (quote.isCached) "cached" else "ok",
                        sourceTime = (if (estimate != null) quote.estimateTime else quote.navDate).takeIf { it.isNotBlank() },
                        currency = "CNY",
                    )
                } else {
                    val quote = when (item.marketType) {
                        MarketType.A_STOCK -> stockRepository.getAStockQuotes(listOf(item.code))
                        MarketType.HK_STOCK -> stockRepository.getHKStockQuotes(listOf(item.code))
                        MarketType.US_STOCK -> stockRepository.getUSStockQuotes(listOf(item.code))
                        MarketType.GOLD -> stockRepository.getGoldQuotes(listOf(item.code))
                        MarketType.FUND -> emptyList()
                    }.firstOrNull { it.code == item.code && it.price.isFinite() && it.price > 0 }
                    if (quote == null) missing else missing.copy(
                        currentPrice = quote.price,
                        changePercent = quote.changePercent,
                        changeAmount = quote.changeAmount,
                        quoteStatus = if (quote.isCached) "cached" else "ok",
                        sourceTime = quote.time.takeIf { it.isNotBlank() },
                        currency = toolCurrency(item.code),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                missing
            }
        }
        return WatchlistSnapshot(items = snapshots, generatedAt = Instant.now().toString())
    }
}
