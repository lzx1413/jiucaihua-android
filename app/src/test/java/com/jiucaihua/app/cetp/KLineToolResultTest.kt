package com.jiucaihua.app.cetp

import com.jiucaihua.app.ai.model.KLineToolSnapshot
import com.jiucaihua.app.ai.model.AlertSnapshot
import com.jiucaihua.app.ai.model.AlertsToolSnapshot
import com.jiucaihua.app.ai.usecase.BuildKLineToolSnapshotUseCase
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.usecase.GetKLineDataUseCase
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class KLineToolResultTest {
    private val serializer = CetpToolResultSerializer()

    @Test
    fun sparseIndicatorsAreOmittedAndNumbersRoundHalfUpWithoutDroppingZero() = runTest {
        val snapshot = buildSnapshot(listOf(point(1.00449).copy(
            open = 1.0045,
            high = 2.6755,
            low = 0.004,
            volume = 0.0,
            changePercent = -1.0045,
        )))
        val json = serializer.toJson(snapshot)
        val result = JSONObject(json)
        val item = result.getJSONArray("points").getJSONObject(0)

        assertFalse(json.contains("null"))
        assertFalse(json.contains('\n'))
        assertFalse(item.has("ma5"))
        assertFalse(item.has("rsi6"))
        assertFalse(item.has("bollUpper"))
        assertEquals(1.005, item.getDouble("open"), 0.0)
        assertEquals(1.004, item.getDouble("close"), 0.0)
        assertEquals(2.676, item.getDouble("high"), 0.0)
        assertEquals(-1.005, item.getDouble("changePercent"), 0.0)
        assertEquals(0.004, item.getDouble("low"), 0.0)
        assertEquals(0.0, item.getDouble("volume"), 0.0)
        assertEquals(2.676, result.getDouble("highestHigh"), 0.0)
        assertEquals(0.004, result.getDouble("lowestLow"), 0.0)
        assertCompactNumbers(result)
    }

    @Test
    fun allIndicatorNumbersAreRoundedAndLatestPointMatchesLastPoint() = runTest {
        val snapshot = buildSnapshot(List(120) { index ->
            point(10.0 + index * 0.1234567).copy(volume = 12345.6789 + index)
        })
        val json = serializer.toJson(snapshot)
        val result = JSONObject(json)
        val points = result.getJSONArray("points")
        val latest = result.getJSONObject("latestPoint")

        assertEquals(120, points.length())
        val last = points.getJSONObject(points.length() - 1)
        assertEquals(last.keys().asSequence().toSet(), latest.keys().asSequence().toSet())
        last.keys().forEach { field -> assertEquals(last.get(field), latest.get(field)) }
        for (field in listOf("ma120", "volumeRatio", "dif", "dea", "macd", "rsi24", "bollUpper")) {
            assertTrue("Missing computed indicator: $field", latest.has(field))
        }
        assertFalse(json.contains("null"))
        assertCompactNumbers(result)
    }

    @Test
    fun indicatorsUseOriginalPrecisionBeforeSnapshotRounding() = runTest {
        val points = listOf(1.0005, 1.0005, 1.0005, 1.0004, 1.0004).map { point(it) }
        val snapshot = buildSnapshot(points)

        // Original mean is 1.00046 -> 1.000; averaging rounded closes would yield 1.001.
        assertEquals(1.0, snapshot.latestPoint!!.ma5!!, 0.0)
        assertEquals(1.001, snapshot.points.first().close, 0.0)
        assertEquals(1.0005, points.first().close, 0.0)
    }

    @Test
    fun emptySeriesOmitsLatestPointAndKeepsEmptyPoints() = runTest {
        val result = JSONObject(serializer.toJson(buildSnapshot(emptyList())))

        assertFalse(result.has("latestPoint"))
        assertEquals(0, result.getInt("pointsCount"))
        assertEquals(0, result.getJSONArray("points").length())
    }

    @Test
    fun allToolSnapshotsUseCompactJsonAndOmitNulls() {
        val json = serializer.toJson(
            AlertsToolSnapshot(
                total = 1,
                enabledCount = 1,
                recentTriggeredCount = 0,
                alerts = listOf(
                    AlertSnapshot(
                        id = 1,
                        code = "sh600519",
                        name = "测试",
                        alertType = "PRICE_ABOVE",
                        threshold = 100.0,
                        actionHint = null,
                        isEnabled = true,
                        lastTriggeredAt = null,
                    )
                ),
            )
        )

        assertFalse(json.contains("null"))
        assertFalse(json.contains('\n'))
        assertFalse(JSONObject(json).getJSONArray("alerts").getJSONObject(0).has("actionHint"))
        assertFalse(JSONObject(json).getJSONArray("alerts").getJSONObject(0).has("lastTriggeredAt"))
    }

    @Test
    fun shortResponseRetainsWarmupHistoryForMa120() = runTest {
        val repository = mock<StockRepository>()
        val data = KLineData("sh600519", "测试", KLinePeriod.DAILY, List(179) { point(it + 1.0) })
        whenever(repository.getKLineData("sh600519", KLinePeriod.DAILY, 179)).thenReturn(data)
        val result = BuildKLineToolSnapshotUseCase(GetKLineDataUseCase(repository), mock())("sh600519", KLinePeriod.DAILY, 60)
        assertEquals(60, result.pointsCount)
        assertEquals(60.5, result.points.first().ma120!!, 0.0)
        assertEquals(119.5, result.points.last().ma120!!, 0.0)
        assertEquals(null, result.latestPoint)
        assertEquals(result.points.minOf { it.low }, result.lowestLow)
    }

    @Test
    fun ohlcvModeOmitsIndicatorsAndDoesNotFetchWarmup() = runTest {
        val repository = mock<StockRepository>()
        whenever(repository.getKLineData("sh600519", KLinePeriod.DAILY, 5)).thenReturn(
            KLineData("sh600519", "测试", KLinePeriod.DAILY, List(5) { point(it + 1.0) }),
        )
        val result = BuildKLineToolSnapshotUseCase(GetKLineDataUseCase(repository), mock())("sh600519", KLinePeriod.DAILY, 5, includeIndicators = false)
        val json = serializer.toJson(result)
        assertFalse(json.contains("ma5"))
        assertFalse(json.contains("latestPoint"))
        assertEquals(5, result.pointsCount)
    }

    @Test
    fun mapsAndNestedObjectsSerializeWithoutNullMembers() {
        val result = JSONObject(serializer.toJson(mapOf("success" to true, "nested" to mapOf("missing" to null, "zero" to 0))))
        assertTrue(result.getBoolean("success"))
        assertFalse(result.getJSONObject("nested").has("missing"))
        assertEquals(0, result.getJSONObject("nested").getInt("zero"))
    }

    private suspend fun buildSnapshot(points: List<KLinePoint>): KLineToolSnapshot {
        val repository = mock<StockRepository>()
        whenever(repository.getKLineData("sh600519", KLinePeriod.DAILY, 239)).thenReturn(
            KLineData("sh600519", "测试证券", KLinePeriod.DAILY, points),
        )
        return BuildKLineToolSnapshotUseCase(GetKLineDataUseCase(repository), mock())(
            "sh600519", KLinePeriod.DAILY, 120, includeLatest = true,
        )
    }

    private fun point(close: Double) = KLinePoint(
        date = "2026-09-25",
        open = close,
        close = close,
        high = close + 0.12345,
        low = close - 0.12345,
        volume = 12345.6789,
    )

    private fun assertCompactNumbers(value: Any) {
        when (value) {
            is JSONObject -> value.keys().forEach { assertCompactNumbers(value.get(it)) }
            is JSONArray -> (0 until value.length()).forEach { assertCompactNumbers(value.get(it)) }
            is Number -> assertTrue(
                "More than three decimal places: $value",
                BigDecimal(value.toString()).stripTrailingZeros().scale() <= 3,
            )
        }
    }
}
