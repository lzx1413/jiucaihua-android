package com.jiucaihua.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DailySnapshotScheduleTest {

    private val closedMarkets = mapOf(
        MarketType.A_STOCK to MarketSession.CLOSED,
        MarketType.HK_STOCK to MarketSession.CLOSED,
    )

    @Test
    fun `records after both markets close on a weekday`() {
        assertTrue(DailySnapshotSchedule.shouldRecord(time("2026-07-20T16:10:00+08:00"), closedMarkets))
    }

    @Test
    fun `does not record during trading or before close time`() {
        assertFalse(DailySnapshotSchedule.shouldRecord(time("2026-07-20T16:09:00+08:00"), closedMarkets))
        assertFalse(
            DailySnapshotSchedule.shouldRecord(
                time("2026-07-20T16:20:00+08:00"),
                closedMarkets + (MarketType.HK_STOCK to MarketSession.TRADING),
            ),
        )
    }

    @Test
    fun `does not record on a weekend or holiday`() {
        assertFalse(DailySnapshotSchedule.shouldRecord(time("2026-07-19T16:20:00+08:00"), closedMarkets))
        assertFalse(
            DailySnapshotSchedule.shouldRecord(
                time("2026-07-20T16:20:00+08:00"),
                closedMarkets + (MarketType.A_STOCK to MarketSession.HOLIDAY),
            ),
        )
    }

    private fun time(value: String): ZonedDateTime = ZonedDateTime.parse(value).withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
}
