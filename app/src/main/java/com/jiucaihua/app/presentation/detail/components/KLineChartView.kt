package com.jiucaihua.app.presentation.detail.components

import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.jiucaihua.app.domain.model.KLineData

private const val RISE_COLOR = 0xFFE53935.toInt()
private const val FALL_COLOR = 0xFF43A047.toInt()
private const val VISIBLE_COUNT = 60

@Composable
fun KLineChartView(
    kLineData: KLineData,
    modifier: Modifier = Modifier,
) {
    if (kLineData.points.isEmpty()) return

    val points = kLineData.points
    val dates = points.map { it.date }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f).toArgb()

    val visibleCount = VISIBLE_COUNT.coerceAtMost(points.size)
    val initialPosition = remember(kLineData) {
        (points.size - visibleCount).coerceAtLeast(0).toFloat()
    }

    val syncHelper = remember { ChartSyncHelper() }

    Column(modifier = modifier.fillMaxWidth()) {
        // K-line candlestick chart
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            factory = { context ->
                CombinedChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    isHighlightFullBarEnabled = false
                    setScaleEnabled(false)
                    isDragEnabled = true
                    isDragDecelerationEnabled = true
                    dragDecelerationFrictionCoef = 0.92f
                    setDrawOrder(arrayOf(CombinedChart.DrawOrder.CANDLE))
                    legend.isEnabled = false

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawLabels(false)
                        setDrawGridLines(true)
                        this.gridColor = gridColor
                        granularity = 1f
                        setLabelCount(4, true)
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        this.gridColor = gridColor
                        this.textColor = textColor
                        textSize = 9f
                        setLabelCount(5, true)
                        setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                    }

                    axisRight.apply {
                        setDrawGridLines(false)
                        setDrawLabels(false)
                    }

                    isDoubleTapToZoomEnabled = false

                    setOnChartGestureListener(object : OnChartGestureListener {
                        override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}
                        override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}
                        override fun onChartLongPressed(me: MotionEvent) {}
                        override fun onChartDoubleTapped(me: MotionEvent) {}
                        override fun onChartSingleTapped(me: MotionEvent) {}
                        override fun onChartFling(me: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}
                        override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
                        override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {
                            syncHelper.syncToVolume()
                        }
                    })
                }
            },
            update = { chart ->
                syncHelper.candleChart = chart

                val candleEntries = points.mapIndexed { i, p ->
                    CandleEntry(
                        i.toFloat(),
                        p.high.toFloat(),
                        p.low.toFloat(),
                        p.open.toFloat(),
                        p.close.toFloat(),
                    )
                }

                val candleDataSet = CandleDataSet(candleEntries, "K线").apply {
                    setDrawValues(false)
                    shadowColorSameAsCandle = true
                    decreasingColor = FALL_COLOR
                    decreasingPaintStyle = Paint.Style.FILL
                    increasingColor = RISE_COLOR
                    increasingPaintStyle = Paint.Style.FILL
                    neutralColor = Color.GRAY
                    shadowWidth = 1f
                    axisDependency = YAxis.AxisDependency.LEFT
                }

                val combinedData = CombinedData().apply {
                    setData(CandleData(candleDataSet))
                }

                chart.data = combinedData
                val priceMin = points.minOf { it.low }
                val priceMax = points.maxOf { it.high }
                val priceRange = priceMax - priceMin
                chart.axisLeft.axisMinimum = (priceMin - priceRange * 0.05).toFloat()
                chart.axisLeft.axisMaximum = (priceMax + priceRange * 0.05).toFloat()

                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMaximum(visibleCount.toFloat())
                chart.setVisibleXRangeMinimum(20f)
                chart.moveViewToX(initialPosition)
                chart.invalidate()
            }
        )

        // Volume chart
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            factory = { context ->
                BarChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                    isHighlightFullBarEnabled = false
                    setScaleEnabled(false)
                    isDragEnabled = true
                    isDragDecelerationEnabled = true
                    dragDecelerationFrictionCoef = 0.92f
                    legend.isEnabled = false

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(true)
                        this.gridColor = gridColor
                        this.textColor = textColor
                        granularity = 1f
                        setLabelCount(4, true)
                        textSize = 9f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                if (idx < 0 || idx >= dates.size) return ""
                                val d = dates[idx]
                                return if (d.length >= 10) d.substring(5) else d
                            }
                        }
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        this.gridColor = gridColor
                        this.textColor = textColor
                        textSize = 9f
                        setLabelCount(3, true)
                        setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return when {
                                    value >= 1_0000_0000 -> "%.1f亿".format(value / 1_0000_0000)
                                    value >= 1_0000 -> "%.0f万".format(value / 1_0000)
                                    else -> "%.0f".format(value)
                                }
                            }
                        }
                    }

                    axisRight.apply {
                        setDrawGridLines(false)
                        setDrawLabels(false)
                    }

                    isDoubleTapToZoomEnabled = false

                    setOnChartGestureListener(object : OnChartGestureListener {
                        override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}
                        override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}
                        override fun onChartLongPressed(me: MotionEvent) {}
                        override fun onChartDoubleTapped(me: MotionEvent) {}
                        override fun onChartSingleTapped(me: MotionEvent) {}
                        override fun onChartFling(me: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}
                        override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
                        override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {
                            syncHelper.syncToCandle()
                        }
                    })
                }
            },
            update = { chart ->
                syncHelper.volumeChart = chart

                val barEntries = points.mapIndexed { i, p ->
                    BarEntry(i.toFloat(), p.volume.toFloat())
                }
                val barColors = points.map { p ->
                    if (p.close >= p.open) RISE_COLOR else FALL_COLOR
                }

                val barDataSet = BarDataSet(barEntries, "成交量").apply {
                    setDrawValues(false)
                    colors = barColors
                    axisDependency = YAxis.AxisDependency.LEFT
                }

                chart.data = BarData(barDataSet).apply { barWidth = 0.7f }
                chart.axisLeft.axisMinimum = 0f
                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMaximum(visibleCount.toFloat())
                chart.setVisibleXRangeMinimum(20f)
                chart.moveViewToX(initialPosition)
                chart.invalidate()
            }
        )
    }
}

private class ChartSyncHelper {
    var candleChart: CombinedChart? = null
    var volumeChart: BarChart? = null
    private var syncing = false

    fun syncToVolume() {
        if (syncing) return
        val cc = candleChart ?: return
        val vc = volumeChart ?: return
        syncing = true
        try {
            vc.moveViewToX(cc.lowestVisibleX)
        } finally {
            syncing = false
        }
    }

    fun syncToCandle() {
        if (syncing) return
        val cc = candleChart ?: return
        val vc = volumeChart ?: return
        syncing = true
        try {
            cc.moveViewToX(vc.lowestVisibleX)
        } finally {
            syncing = false
        }
    }
}
