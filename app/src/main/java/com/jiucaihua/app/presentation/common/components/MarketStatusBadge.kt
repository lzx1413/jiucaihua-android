package com.jiucaihua.app.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketType

@Composable
fun MarketStatusBadge(
    sessions: Map<MarketType, MarketSession>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sessions.forEach { (type, session) ->
            MarketBadge(marketType = type, session = session)
        }
    }
}

@Composable
private fun MarketBadge(
    marketType: MarketType,
    session: MarketSession,
) {
    val (bgColor, textColor) = when (session) {
        MarketSession.TRADING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
        MarketSession.LUNCH_BREAK -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.tertiary
        MarketSession.CLOSED, MarketSession.HOLIDAY ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (session == MarketSession.TRADING) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = textColor,
                    modifier = Modifier.size(6.dp),
                ) {}
            }
            Text(
                text = "${marketType.label} ${session.label}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}
