package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.remote.api.HolidayApi
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MarketCalendarRepositoryImpl @Inject constructor(
    private val holidayApi: HolidayApi,
) : MarketCalendarRepository {

    private var cachedHolidayDate: LocalDate? = null
    private var cachedIsHoliday: Boolean? = null

    override suspend fun getMarketSessions(): Map<MarketType, MarketSession> {
        val isHoliday = isTodayHoliday()
        val shanghai = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        val hongKong = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"))

        return mapOf(
            MarketType.A_STOCK to calcAStockSession(shanghai, isHoliday),
            MarketType.HK_STOCK to calcHKStockSession(hongKong, isHoliday),
        )
    }

    override suspend fun isTodayHoliday(): Boolean {
        val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
        if (cachedHolidayDate == today && cachedIsHoliday != null) {
            return cachedIsHoliday!!
        }

        return try {
            val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val response = holidayApi.getHolidayInfo(dateStr)
            val json = JSONObject(response)
            val typeInfo = json.optJSONObject("type")
            val type = typeInfo?.optInt("type", -1) ?: -1
            // type: 0=工作日, 1=周末, 2=法定节假日, 3=调休上班日
            val holiday = type == 1 || type == 2
            cachedHolidayDate = today
            cachedIsHoliday = holiday
            holiday
        } catch (_: Exception) {
            // API failure: fall back to weekend check
            val dow = today.dayOfWeek
            dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
        }
    }

    private fun calcAStockSession(now: ZonedDateTime, isHoliday: Boolean): MarketSession {
        val dow = now.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return MarketSession.CLOSED
        if (isHoliday) return MarketSession.HOLIDAY

        val time = now.toLocalTime()
        return when {
            time < LocalTime.of(9, 15) -> MarketSession.CLOSED
            time < LocalTime.of(11, 30) -> MarketSession.TRADING
            time < LocalTime.of(13, 0) -> MarketSession.LUNCH_BREAK
            time < LocalTime.of(15, 0) -> MarketSession.TRADING
            else -> MarketSession.CLOSED
        }
    }

    private fun calcHKStockSession(now: ZonedDateTime, isHoliday: Boolean): MarketSession {
        val dow = now.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return MarketSession.CLOSED
        if (isHoliday) return MarketSession.HOLIDAY

        val time = now.toLocalTime()
        return when {
            time < LocalTime.of(9, 0) -> MarketSession.CLOSED
            time < LocalTime.of(12, 0) -> MarketSession.TRADING
            time < LocalTime.of(13, 0) -> MarketSession.LUNCH_BREAK
            time < LocalTime.of(16, 0) -> MarketSession.TRADING
            else -> MarketSession.CLOSED
        }
    }
}
