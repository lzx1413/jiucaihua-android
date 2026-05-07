package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.WatchlistItemSnapshot
import com.jiucaihua.app.ai.model.WatchlistSnapshot
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.first
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BuildWatchlistSnapshotUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(): WatchlistSnapshot {
        val items = watchlistRepository.getAllWatchlist().first()
        val snapshots = items.map { item ->
            val quote = try {
                fetchQuote(item.code, item.marketType)
            } catch (_: Exception) {
                null
            }
            WatchlistItemSnapshot(
                code = item.code,
                name = item.name,
                marketType = item.marketType.name,
                currentPrice = quote?.price ?: 0.0,
                changePercent = quote?.changePercent ?: 0.0,
                changeAmount = quote?.changeAmount ?: 0.0,
            )
        }
        return WatchlistSnapshot(
            items = snapshots,
            generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
    }

    private suspend fun fetchQuote(code: String, marketType: com.jiucaihua.app.domain.model.MarketType) =
        when (marketType) {
            com.jiucaihua.app.domain.model.MarketType.A_STOCK,
            com.jiucaihua.app.domain.model.MarketType.FUND -> stockRepository.getAStockQuotes(listOf(code))
            com.jiucaihua.app.domain.model.MarketType.HK_STOCK -> stockRepository.getHKStockQuotes(listOf(code))
            com.jiucaihua.app.domain.model.MarketType.US_STOCK -> stockRepository.getUSStockQuotes(listOf(code))
            com.jiucaihua.app.domain.model.MarketType.GOLD -> stockRepository.getGoldQuotes(listOf(code))
        }.firstOrNull()
}
