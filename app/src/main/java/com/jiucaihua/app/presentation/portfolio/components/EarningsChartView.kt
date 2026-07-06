package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.presentation.theme.RiseRed
import com.jiucaihua.app.presentation.theme.FallGreen
import java.text.SimpleDateFormat
import kotlin.math.abs
import kotlin.math.max

private val BenchmarkColor = Color(0xFFFF9800)

@Composable
fun EarningsChartView(
    snapshots: List<PortfolioSnapshot>,
    selectedRange: ChartRange,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) return

    val hasBenchmark = snapshots.any { it.benchmarkPercent != 0.0 }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val positiveColor = RiseRed
    val negativeColor = FallGreen
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val fillColor = lineColor.copy(alpha = 0.15f)

    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
    )

    val dateFormat = SimpleDateFormat("MM/dd", LocalLocale.current.platformLocale)

    val earningsPercentValues = snapshots.map { it.totalEarningsPercent }
    val benchmarkPercentValues = if (hasBenchmark) snapshots.map { it.benchmarkPercent } else null

    // Calculate Y-axis range considering both lines
    val allYValues = if (benchmarkPercentValues != null) {
        earningsPercentValues + benchmarkPercentValues
    } else earningsPercentValues

    val minY = allYValues.minOrNull() ?: 0.0
    val maxY = allYValues.maxOrNull() ?: 0.0
    val range = max(maxY - minY, 1.0)
    val paddingRatio = 0.1
    val adjustedMin = minY - range * paddingRatio
    val adjustedMax = maxY + range * paddingRatio
    val adjustedRange = max(adjustedMax - adjustedMin, 1.0)

    val dateSkipCount = when (selectedRange) {
        ChartRange.SEVEN_DAYS -> 1
        ChartRange.THIRTY_DAYS -> 5
        ChartRange.NINETY_DAYS -> 15
        ChartRange.ALL -> if (snapshots.size > 30) snapshots.size / 6 else 1
    }

    val chartHeight = 200.dp

    Column(modifier = modifier) {
        // Legend row
        if (hasBenchmark) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(modifier = Modifier.size(16.dp, 3.dp)) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.portfolio_summary_holding_earnings),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Canvas(modifier = Modifier.size(16.dp, 3.dp)) {
                    drawLine(
                        color = BenchmarkColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.benchmark_csi300),
                    style = MaterialTheme.typography.labelSmall,
                    color = BenchmarkColor,
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(horizontal = 16.dp),
        ) {
            val chartWidth = size.width
            val chartHeightPx = size.height
            val leftPadding = 50f
            val rightPadding = 10f
            val topPadding = 15f
            val bottomPadding = 35f
            val drawableWidth = chartWidth - leftPadding - rightPadding
            val drawableHeight = chartHeightPx - topPadding - bottomPadding
            fun drawBoundedText(
                text: String,
                preferredTopLeft: Offset,
                style: TextStyle,
            ) {
                val textLayout = textMeasurer.measure(text, style)
                val boundedX = preferredTopLeft.x.coerceIn(
                    0f,
                    max(chartWidth - textLayout.size.width, 0f),
                )
                val boundedY = preferredTopLeft.y.coerceIn(
                    0f,
                    max(chartHeightPx - textLayout.size.height, 0f),
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(boundedX, boundedY),
                )
            }

            // Draw zero line
            val zeroY = topPadding + drawableHeight * ((adjustedMax - 0.0) / adjustedRange).toFloat()
            drawLine(
                color = zeroLineColor,
                start = Offset(leftPadding, zeroY),
                end = Offset(chartWidth - rightPadding, zeroY),
                strokeWidth = 1f,
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "0%",
                topLeft = Offset(0f, zeroY - 6f),
                style = textStyle,
            )

            // Draw grid lines
            val gridCount = 4
            for (i in 1..gridCount) {
                val y = topPadding + drawableHeight * (i.toFloat() / gridCount)
                val gridValue = adjustedMax - adjustedRange * i / gridCount
                if (abs(y - zeroY) > 1f && gridValue.toInt() != 0) {
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(chartWidth - rightPadding, y),
                        strokeWidth = 0.5f,
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${gridValue.toInt()}%",
                        topLeft = Offset(0f, y - 6f),
                        style = textStyle,
                    )
                }
            }

            // Calculate portfolio points
            val pointCount = snapshots.size
            val portfolioPoints = snapshots.mapIndexed { index, snapshot ->
                val x = leftPadding + drawableWidth * index.toFloat() / max(pointCount - 1, 1)
                val yPercent = snapshot.totalEarningsPercent
                val y = topPadding + drawableHeight * ((adjustedMax - yPercent) / adjustedRange).toFloat()
                Offset(x, y)
            }

            // Draw fill area
            val fillPath = Path().apply {
                moveTo(portfolioPoints.first().x, zeroY)
                for (point in portfolioPoints) {
                    lineTo(point.x, point.y)
                }
                lineTo(portfolioPoints.last().x, zeroY)
                close()
            }
            drawPath(
                path = fillPath,
                color = fillColor,
                style = Fill,
            )

            // Draw portfolio line
            val linePath = Path().apply {
                moveTo(portfolioPoints.first().x, portfolioPoints.first().y)
                for (i in 1 until portfolioPoints.size) {
                    lineTo(portfolioPoints[i].x, portfolioPoints[i].y)
                }
            }

            val lastEarningsPercent = earningsPercentValues.last()
            val chartLineColor = if (lastEarningsPercent >= 0) positiveColor else negativeColor

            drawPath(
                path = linePath,
                color = chartLineColor,
                style = Stroke(width = 2f),
            )

            // Draw benchmark line (CSI 300)
            if (benchmarkPercentValues != null && hasBenchmark) {
                val benchmarkPoints = snapshots.mapIndexed { index, snapshot ->
                    val x = leftPadding + drawableWidth * index.toFloat() / max(pointCount - 1, 1)
                    val yPercent = snapshot.benchmarkPercent
                    val y = topPadding + drawableHeight * ((adjustedMax - yPercent) / adjustedRange).toFloat()
                    Offset(x, y)
                }

                val benchmarkPath = Path().apply {
                    moveTo(benchmarkPoints.first().x, benchmarkPoints.first().y)
                    for (i in 1 until benchmarkPoints.size) {
                        lineTo(benchmarkPoints[i].x, benchmarkPoints[i].y)
                    }
                }
                drawPath(
                    path = benchmarkPath,
                    color = BenchmarkColor,
                    style = Stroke(width = 2f),
                )

                // Draw benchmark last point indicator
                if (benchmarkPoints.isNotEmpty()) {
                    val lastBenchmark = benchmarkPoints.last()
                    drawCircle(
                        color = BenchmarkColor,
                        radius = 3f,
                        center = lastBenchmark,
                    )
                    val lastBenchmarkValue = benchmarkPercentValues.last()
                    val benchmarkValueText = "${(lastBenchmarkValue * 100).toInt() / 100.0}%"
                    drawBoundedText(
                        text = benchmarkValueText,
                        preferredTopLeft = Offset(lastBenchmark.x + 5f, lastBenchmark.y + 2f),
                        style = TextStyle(
                            color = BenchmarkColor,
                            fontSize = 10.sp,
                        ),
                    )
                }
            }

            // Draw date labels
            snapshots.forEachIndexed { index, snapshot ->
                if (index % dateSkipCount == 0 || index == snapshots.size - 1) {
                    val x = portfolioPoints[index].x
                    val dateLabel = dateFormat.format(snapshot.timestamp)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = dateLabel,
                        topLeft = Offset(x - 15f, chartHeightPx - bottomPadding + 5f),
                        style = textStyle,
                    )
                }
            }

            // Draw portfolio last value indicator
            if (portfolioPoints.isNotEmpty()) {
                val lastPoint = portfolioPoints.last()
                drawCircle(
                    color = chartLineColor,
                    radius = 3f,
                    center = lastPoint,
                )
                val lastValueText = "${(lastEarningsPercent * 100).toInt() / 100.0}%"
                drawBoundedText(
                    text = lastValueText,
                    preferredTopLeft = Offset(lastPoint.x + 5f, lastPoint.y - 10f),
                    style = TextStyle(
                        color = chartLineColor,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}
