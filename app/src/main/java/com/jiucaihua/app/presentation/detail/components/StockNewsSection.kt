package com.jiucaihua.app.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.NewsSource
import com.jiucaihua.app.presentation.theme.FallGreen
import com.jiucaihua.app.presentation.theme.RiseRed

@Composable
fun StockNewsSection(
    title: String = "资讯",
    articles: List<NewsFlash>,
    isLoading: Boolean,
    error: String?,
    onArticleClick: (NewsFlash) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(
                        text = error,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                articles.isEmpty() -> {
                    Text(
                        text = "暂无资讯",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        articles.forEachIndexed { index, article ->
                            StockArticleItem(
                                article = article,
                                onClick = { onArticleClick(article) },
                            )
                            if (index != articles.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StockArticleItem(
    article: NewsFlash,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NewsSourceDot(sourceType = article.sourceType)
            Text(
                text = article.source,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (article.impact.isNotBlank()) {
                Text(
                    text = article.impact,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (article.impact == "利好") RiseRed else FallGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (article.time.isNotBlank()) {
                Text(
                    text = article.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = article.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewsSourceDot(sourceType: NewsSource) {
    val color = when (sourceType) {
        NewsSource.STCN -> Color(0xFFE53935)
        NewsSource.XUANGUBAO -> Color(0xFF1565C0)
        NewsSource.CLS -> Color(0xFFFF6F00)
        NewsSource.WALLSTREETCN -> Color(0xFF00897B)
        NewsSource.JIN10 -> Color(0xFF6A1B9A)
        NewsSource.EASTMONEY -> Color(0xFF2E7D32)
        NewsSource.JIUYAN -> Color(0xFF5D4037)
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape),
    )
}
