package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import javax.inject.Inject

class IsMarketOpenUseCase @Inject constructor(
    private val marketCalendarRepository: MarketCalendarRepository,
) {
    suspend fun getMarketSessions(): Map<MarketType, MarketSession> {
        return marketCalendarRepository.getMarketSessions()
    }

    suspend fun isAnyMarketTrading(): Boolean {
        return marketCalendarRepository.getMarketSessions().values.any { it == MarketSession.TRADING }
    }

    suspend fun isMarketTrading(marketType: MarketType): Boolean {
        val sessions = marketCalendarRepository.getMarketSessions()
        return sessions[marketType] == MarketSession.TRADING
    }
}
