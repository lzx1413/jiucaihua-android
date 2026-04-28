package com.jiucaihua.app.presentation.market.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiucaihua.app.domain.model.MarketIndex
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun IndexCardGrid(
    indices: List<MarketIndex>,
    onIndexClick: (MarketIndex) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (indices.isEmpty()) return

    val rowCount = (indices.size + 2) / 3
    val gridHeight = (rowCount * 95 + (rowCount - 1) * 8 + 16).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .height(gridHeight)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(indices) { index ->
            IndexCard(
                index = index,
                onClick = { onIndexClick(index) },
            )
        }
    }
}

@Composable
fun IndexCard(
    index: MarketIndex,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changeColor = when {
        index.changePercent > 0 -> RiseRed
        index.changePercent < 0 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = index.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (index.price > 0) {
                Text(
                    text = "%.2f".format(index.price),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = changeColor,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val sign = if (index.changeAmount >= 0) "+" else ""
                    Text(
                        text = "$sign${"%.2f".format(index.changePercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = changeColor,
                    )
                }
            } else {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
