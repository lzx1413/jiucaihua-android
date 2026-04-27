package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType

interface MarketCalendarRepository {

    suspend fun getMarketSessions(): Map<MarketType, MarketSession>

    suspend fun isTodayHoliday(): Boolean
}
