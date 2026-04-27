package com.jiucaihua.app.presentation.common.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun PriceChangeText(
    value: Double,
    isPercent: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier,
) {
    val color = when {
        value > 0 -> RiseRed
        value < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = if (isPercent) {
        val sign = if (value >= 0) "+" else ""
        "$sign%.2f%%".format(value)
    } else {
        val sign = if (value >= 0) "+" else ""
        "$sign%.2f".format(value)
    }

    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
    )
}
