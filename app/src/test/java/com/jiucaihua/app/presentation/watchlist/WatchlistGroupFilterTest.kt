package com.jiucaihua.app.presentation.watchlist

import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.WatchlistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistGroupFilterTest {

    @Test
    fun `filters immediately from the complete watchlist when group changes`() {
        val items = listOf(
            item(id = 1, group = "股票"),
            item(id = 2, group = "基金"),
            item(id = 3, group = "股票"),
        )

        assertEquals(listOf(2L), filterWatchlistItems(items, "基金").map { it.id })
        assertEquals(listOf(1L, 2L, 3L), filterWatchlistItems(items, null).map { it.id })
    }

    private fun item(id: Long, group: String) = WatchlistItem(
        id = id,
        code = id.toString(),
        name = "测试$id",
        marketType = MarketType.FUND,
        group = group,
    )
}
