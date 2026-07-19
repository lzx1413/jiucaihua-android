package com.jiucaihua.app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/** The earliest Shanghai time at which both A-share and Hong Kong markets have closed. */
object DailySnapshotSchedule {
    private val closeSnapshotTime = LocalTime.of(16, 10)

    fun shouldRecord(now: ZonedDateTime, sessions: Map<MarketType, MarketSession>): Boolean {
        if (now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY) return false
        if (now.toLocalTime() < closeSnapshotTime) return false

        val trackedSessions = listOf(MarketType.A_STOCK, MarketType.HK_STOCK)
            .mapNotNull { sessions[it] }
        return trackedSessions.isNotEmpty() && trackedSessions.none { it == MarketSession.HOLIDAY || it == MarketSession.TRADING }
    }
}
