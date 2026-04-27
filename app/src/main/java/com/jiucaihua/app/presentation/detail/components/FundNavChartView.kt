package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.jiucaihua.app.domain.model.KLineData

private const val LINE_COLOR = 0xFF1565C0.toInt()
private const val VISIBLE_COUNT = 60

@Composable
fun FundNavChartView(
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
            .height(300.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setScaleEnabled(true)
                setPinchZoom(true)
                legend.isEnabled = false
                isDoubleTapToZoomEnabled = false

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
            }
        },
        update = { chart ->
            val entries = points.mapIndexed { i, p ->
                Entry(i.toFloat(), p.close.toFloat())
            }

            val dataSet = LineDataSet(entries, "净值走势").apply {
                color = LINE_COLOR
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 1.5f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = LINE_COLOR
                fillAlpha = 30
                axisDependency = YAxis.AxisDependency.LEFT
            }

            chart.data = LineData(dataSet)

            val minNav = points.minOf { it.close }
            val maxNav = points.maxOf { it.close }
            val range = maxNav - minNav
            chart.axisLeft.axisMinimum = (minNav - range * 0.05).toFloat()
            chart.axisLeft.axisMaximum = (maxNav + range * 0.05).toFloat()

            chart.setVisibleXRangeMaximum(VISIBLE_COUNT.toFloat())
            chart.moveViewToX((points.size - VISIBLE_COUNT).coerceAtLeast(0).toFloat())
            chart.invalidate()
        }
    )
}
