package com.jiucaihua.app.presentation.market.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.presentation.i18n.localizedLabel

@Composable
fun USStockKLineImageView(
    code: String,
    period: KLinePeriod,
    modifier: Modifier = Modifier,
) {
    val indexCode = code.removePrefix("usr_")
    val timestamp = remember { System.currentTimeMillis() }

    val imageUrl = when (period) {
        KLinePeriod.DAILY -> "https://image.sinajs.cn/newchart/v5/usstock/daily/${indexCode}.gif?t=$timestamp"
        KLinePeriod.WEEKLY -> "https://image.sinajs.cn/newchart/v5/usstock/weekly/${indexCode}.gif?t=$timestamp"
        KLinePeriod.MONTHLY -> "https://image.sinajs.cn/newchart/v5/usstock/monthly/${indexCode}.gif?t=$timestamp"
    }

    val periodLabel = period.localizedLabel()

    Text(
        text = stringResource(R.string.kline_chart_title, periodLabel),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(R.string.kline_chart),
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 8.dp),
    )
}
