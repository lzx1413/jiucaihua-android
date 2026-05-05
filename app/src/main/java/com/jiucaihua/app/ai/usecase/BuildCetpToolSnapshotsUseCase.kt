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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class BuildMarketIndicesSnapshotUseCase @Inject constructor(
    private val marketRepository: MarketRepository,
) {
    suspend operator fun invoke(market: String?): MarketIndicesSnapshot {
        val allGroups = listOf(
            MarketIndexGroup("A_STOCK", "A股", marketRepository.getAStockIndices()),
            MarketIndexGroup("HK_STOCK", "港股", marketRepository.getHKStockIndices()),
            MarketIndexGroup("US_STOCK", "美股", marketRepository.getUSStockIndices()),
            MarketIndexGroup("GOLD", "黄金", marketRepository.getGoldIndices()),
        )
        val groups = if (market != null) {
            allGroups.filter { it.market.equals(market, ignoreCase = true) }
        } else {
            allGroups
        }
        return MarketIndicesSnapshot(
            generatedAt = timestampFormatter.format(Date()),
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
            generatedAt = timestampFormatter.format(Date()),
            updateTime = data.updateTime,
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
            generatedAt = timestampFormatter.format(Date()),
            keyword = keyword,
            total = results.size,
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
            generatedAt = timestampFormatter.format(Date()),
            isTodayHoliday = isHoliday,
            sessions = sessions.mapKeys { it.key.name }.mapValues { it.value.label },
            hkdToCnyRate = rate,
        )
    }
}

private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
