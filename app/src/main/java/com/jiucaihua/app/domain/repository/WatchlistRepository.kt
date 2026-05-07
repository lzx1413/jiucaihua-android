package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getAllWatchlist(): Flow<List<WatchlistItem>>
    suspend fun addWatchlistItem(code: String, name: String, marketType: MarketType): Long
    suspend fun removeWatchlistItem(id: Long)
    suspend fun isWatched(code: String): Boolean
}
