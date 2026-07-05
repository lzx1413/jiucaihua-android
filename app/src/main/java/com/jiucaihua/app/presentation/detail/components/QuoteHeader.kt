package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.StockQuote
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed
import java.util.Locale

@Composable
fun QuoteHeader(
    quote: StockQuote,
    modifier: Modifier = Modifier,
) {
    val changeColor = when {
        quote.changePercent > 0 -> RiseRed
        quote.changePercent < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val currencyPrefix = when (quote.marketType) {
        MarketType.HK_STOCK -> "HK$"
        else -> "¥"
    }

    val isFund = quote.marketType == MarketType.FUND

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val priceLabel = if (isFund) stringResource(R.string.quote_estimated_nav) else ""
        if (isFund) {
            Text(
                text = priceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = if (isFund) "%.4f".format(quote.price)
                   else "$currencyPrefix%.2f".format(quote.price),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = changeColor,
        )

        Row(modifier = Modifier.padding(top = 4.dp)) {
            val sign = if (quote.changeAmount >= 0) "+" else ""
            Text(
                text = "$sign%.4f".format(quote.changeAmount),
                style = MaterialTheme.typography.bodyLarge,
                color = changeColor,
            )
            Text(
                text = "  $sign%.2f%%".format(quote.changePercent),
                style = MaterialTheme.typography.bodyLarge,
                color = changeColor,
            )
        }

        if (!isFund) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    QuoteInfoRow(stringResource(R.string.quote_open), formatPrice(quote.open))
                    QuoteInfoRow(stringResource(R.string.quote_yest_close), formatPrice(quote.yestClose))
                    QuoteInfoRow(stringResource(R.string.quote_high), formatPrice(quote.high))
                }
                Column {
                    QuoteInfoRow(stringResource(R.string.quote_low), formatPrice(quote.low))
                    QuoteInfoRow(stringResource(R.string.quote_volume), formatVolume(quote.volume))
                    QuoteInfoRow(stringResource(R.string.quote_amount), formatAmount(quote.amount))
                }
            }
        } else {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuoteInfoRow(stringResource(R.string.quote_previous_nav), "%.4f".format(quote.yestClose))
            }
        }
    }
}

@Composable
private fun QuoteInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label  ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatPrice(price: Double): String = "%.2f".format(price)

private fun formatVolume(volume: Double): String {
    if (!isChineseLocale()) {
        return when {
            volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
            volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
            volume >= 1_000 -> "%.2fK".format(volume / 1_000)
            else -> "%.0f".format(volume)
        }
    }
    return when {
        volume >= 100_000_000 -> "%.2f亿".format(volume / 100_000_000)
        volume >= 10_000 -> "%.2f万".format(volume / 10_000)
        else -> "%.0f".format(volume)
    }
}

private fun formatAmount(amount: Double): String {
    if (!isChineseLocale()) {
        return when {
            amount >= 1_000_000_000 -> "%.2fB".format(amount / 1_000_000_000)
            amount >= 1_000_000 -> "%.2fM".format(amount / 1_000_000)
            amount >= 1_000 -> "%.2fK".format(amount / 1_000)
            else -> "%.0f".format(amount)
        }
    }
    return when {
        amount >= 100_000_000 -> "%.2f亿".format(amount / 100_000_000)
        amount >= 10_000 -> "%.2f万".format(amount / 10_000)
        else -> "%.0f".format(amount)
    }
}

private fun isChineseLocale(): Boolean = Locale.getDefault().language == Locale.CHINESE.language
