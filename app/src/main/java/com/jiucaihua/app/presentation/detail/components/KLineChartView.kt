package com.jiucaihua.app.presentation.detail.components

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        factory = { context ->
            CombinedChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                isHighlightFullBarEnabled = false
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawOrder(
                    arrayOf(
                        CombinedChart.DrawOrder.BAR,
                        CombinedChart.DrawOrder.CANDLE,
                    )
                )
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
                    setLabelCount(5, true)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                }

                axisRight.apply {
                    setDrawGridLines(false)
                    setDrawLabels(false)
                }

                setVisibleXRangeMaximum(VISIBLE_COUNT.toFloat())
                isDoubleTapToZoomEnabled = false
            }
        },
        update = { chart ->
            val candleEntries = points.mapIndexed { i, p ->
                CandleEntry(
                    i.toFloat(),
                    p.high.toFloat(),
                    p.low.toFloat(),
                    p.open.toFloat(),
                    p.close.toFloat(),
                )
            }

            val maxVolume = points.maxOf { it.volume }
            val priceMin = points.minOf { it.low }
            val priceMax = points.maxOf { it.high }
            val priceRange = priceMax - priceMin
            val volumeScale = if (maxVolume > 0) (priceRange * 0.25) / maxVolume else 0.0

            val barEntries = points.mapIndexed { i, p ->
                BarEntry(i.toFloat(), (p.volume * volumeScale).toFloat() + priceMin.toFloat())
            }
            val barColors = points.map { p ->
                if (p.close >= p.open) RISE_COLOR else FALL_COLOR
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

            val barDataSet = BarDataSet(barEntries, "成交量").apply {
                setDrawValues(false)
                colors = barColors
                axisDependency = YAxis.AxisDependency.LEFT
            }

            val combinedData = CombinedData().apply {
                setData(CandleData(candleDataSet))
                setData(BarData(barDataSet).apply { barWidth = 0.7f })
            }

            chart.data = combinedData
            chart.axisLeft.axisMinimum = (priceMin - priceRange * 0.05).toFloat()
            chart.axisLeft.axisMaximum = (priceMax + priceRange * 0.05).toFloat()

            chart.setVisibleXRangeMaximum(VISIBLE_COUNT.toFloat())
            chart.moveViewToX((points.size - VISIBLE_COUNT).coerceAtLeast(0).toFloat())
            chart.invalidate()
        }
    )
}
