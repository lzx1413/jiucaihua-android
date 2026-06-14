package com.jiucaihua.app.presentation.portfolio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.PortfolioSnapshot
import com.jiucaihua.app.presentation.theme.RiseRed
import com.jiucaihua.app.presentation.theme.FallGreen
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun EarningsChartView(
    snapshots: List<PortfolioSnapshot>,
    selectedRange: ChartRange,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) return

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

    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    val earningsPercentValues = snapshots.map { it.totalEarningsPercent }
    val minEarnings = earningsPercentValues.minOrNull() ?: 0.0
    val maxEarnings = earningsPercentValues.maxOrNull() ?: 0.0
    val range = max(maxEarnings - minEarnings, 1.0)
    val paddingRatio = 0.1
    val adjustedMin = minEarnings - range * paddingRatio
    val adjustedMax = maxEarnings + range * paddingRatio
    val adjustedRange = max(adjustedMax - adjustedMin, 1.0)

    val dateSkipCount = when (selectedRange) {
        ChartRange.SEVEN_DAYS -> 1
        ChartRange.THIRTY_DAYS -> 5
        ChartRange.NINETY_DAYS -> 15
        ChartRange.ALL -> if (snapshots.size > 30) snapshots.size / 6 else 1
    }

    val chartHeight = 180.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(horizontal = 16.dp),
    ) {
        val chartWidth = size.width
        val chartHeightPx = size.height
        val leftPadding = 40f
        val rightPadding = 10f
        val topPadding = 15f
        val bottomPadding = 25f
        val drawableWidth = chartWidth - leftPadding - rightPadding
        val drawableHeight = chartHeightPx - topPadding - bottomPadding

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
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(chartWidth - rightPadding, y),
                strokeWidth = 0.5f,
            )
            val gridValue = adjustedMax - adjustedRange * i / gridCount
            drawText(
                textMeasurer = textMeasurer,
                text = "${gridValue.toInt()}%",
                topLeft = Offset(0f, y - 6f),
                style = textStyle,
            )
        }

        // Calculate points
        val pointCount = snapshots.size
        val points = snapshots.mapIndexed { index, snapshot ->
            val x = leftPadding + drawableWidth * index.toFloat() / max(pointCount - 1, 1)
            val yPercent = snapshot.totalEarningsPercent
            val y = topPadding + drawableHeight * ((adjustedMax - yPercent) / adjustedRange).toFloat()
            Offset(x, y)
        }

        // Draw fill area
        val fillPath = Path().apply {
            moveTo(points.first().x, zeroY)
            for (point in points) {
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, zeroY)
            close()
        }
        drawPath(
            path = fillPath,
            color = fillColor,
            style = Fill,
        )

        // Draw line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val lastEarningsPercent = earningsPercentValues.last()
        val chartLineColor = if (lastEarningsPercent >= 0) positiveColor else negativeColor

        drawPath(
            path = linePath,
            color = chartLineColor,
            style = Stroke(width = 2f),
        )

        // Draw date labels
        snapshots.forEachIndexed { index, snapshot ->
            if (index % dateSkipCount == 0 || index == snapshots.size - 1) {
                val x = points[index].x
                val dateLabel = dateFormat.format(snapshot.timestamp)
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateLabel,
                    topLeft = Offset(x - 15f, chartHeightPx - bottomPadding + 5f),
                    style = textStyle,
                )
            }
        }

        // Draw last value indicator
        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            drawCircle(
                color = chartLineColor,
                radius = 3f,
                center = lastPoint,
            )
            val lastValueText = "${(lastEarningsPercent * 100).toInt() / 100.0}%"
            drawText(
                textMeasurer = textMeasurer,
                text = lastValueText,
                topLeft = Offset(lastPoint.x + 5f, lastPoint.y - 10f),
                style = TextStyle(
                    color = chartLineColor,
                    fontSize = 11.sp,
                ),
            )
        }
    }
}
