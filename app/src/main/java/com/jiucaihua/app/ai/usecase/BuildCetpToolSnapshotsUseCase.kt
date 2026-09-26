package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.FundFlowSnapshot
import com.jiucaihua.app.ai.model.MarketIndexGroup
import com.jiucaihua.app.ai.model.MarketIndicesSnapshot
import com.jiucaihua.app.ai.model.MarketStatusSnapshot
import com.jiucaihua.app.ai.model.SearchResultEntry
import com.jiucaihua.app.ai.model.SearchResultsSnapshot
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.MarketRepository
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import javax.inject.Inject

class BuildMarketIndicesSnapshotUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    suspend operator fun invoke(market: String?): MarketIndicesSnapshot {
        val groups = when (market?.uppercase()) {
            "A_STOCK" -> listOf(MarketIndexGroup("A_STOCK", "A股", marketRepository.getAStockIndices().map { it.toToolItem() }))
            "HK_STOCK" -> listOf(MarketIndexGroup("HK_STOCK", "港股", marketRepository.getHKStockIndices().map { it.toToolItem() }))
            "US_STOCK" -> listOf(MarketIndexGroup("US_STOCK", "美股", marketRepository.getUSStockIndices().map { it.toToolItem() }))
            "GOLD" -> listOf(MarketIndexGroup("GOLD", "黄金", marketRepository.getGoldIndices().map { it.toToolItem() }))
            else -> listOf(
                MarketIndexGroup("A_STOCK", "A股", marketRepository.getAStockIndices().map { it.toToolItem() }),
                MarketIndexGroup("HK_STOCK", "港股", marketRepository.getHKStockIndices().map { it.toToolItem() }),
                MarketIndexGroup("US_STOCK", "美股", marketRepository.getUSStockIndices().map { it.toToolItem() }),
                MarketIndexGroup("GOLD", "黄金", marketRepository.getGoldIndices().map { it.toToolItem() }),
            )
        }
        return MarketIndicesSnapshot(
            generatedAt = java.time.Instant.now().toString(),
            groups = groups,
        )
    }
}

class BuildFundFlowSnapshotUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    suspend operator fun invoke(): FundFlowSnapshot {
        val data = marketRepository.getFundFlowData()
        return FundFlowSnapshot(
            generatedAt = java.time.Instant.now().toString(),
            updateTime = data.updateTime.takeIf { it.isNotBlank() },
            northFlow = data.northFlow,
            southFlow = data.southFlow,
        )
    }
}

class BuildSearchResultsSnapshotUseCase @Inject constructor(
    private val searchRepository: SecuritySearchRepository,
) {
    suspend operator fun invoke(keyword: String, limit: Int = 20): SearchResultsSnapshot {
        val results = searchRepository.search(keyword, limit)
        return SearchResultsSnapshot(
            generatedAt = java.time.Instant.now().toString(),
            keyword = keyword,
            total = results.size,
            limit = limit,
            possiblyTruncated = results.size >= limit,
            results = results.map {
                SearchResultEntry(
                    code = it.code,
                    displayCode = it.displayCode,
                    name = it.name,
                    marketType = it.marketType.name,
                )
            },
        )
    }
}

class BuildMarketStatusSnapshotUseCase @Inject constructor(
    private val marketCalendarRepository: MarketCalendarRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    suspend operator fun invoke(): MarketStatusSnapshot {
        val sessions = marketCalendarRepository.getMarketSessions()
        val isHoliday = marketCalendarRepository.isTodayHoliday()
        val rate = exchangeRateRepository.getHkdToCnyRate()
        return MarketStatusSnapshot(
            generatedAt = java.time.Instant.now().toString(),
            isTodayHoliday = isHoliday,
            sessions = sessions.mapKeys { it.key.name }.mapValues { it.value.label },
            hkdToCnyRate = rate,
        )
    }
}



private fun com.jiucaihua.app.domain.model.MarketIndex.toToolItem() = com.jiucaihua.app.ai.model.MarketIndexToolItem(
    code = code,
    name = name,
    price = price.takeIf { it > 0 && it.isFinite() },
    changePercent = changePercent.takeIf { price > 0 && it.isFinite() },
    changeAmount = changeAmount.takeIf { price > 0 && it.isFinite() },
    sourceTime = time.takeIf { it.isNotBlank() },
    priceUnit = if (marketType == MarketType.GOLD) "provider_native" else "index_points",
    currency = if (marketType == MarketType.GOLD) toolCurrency(code) else null,
    status = if (price > 0 && price.isFinite()) "ok" else "unavailable",
)
