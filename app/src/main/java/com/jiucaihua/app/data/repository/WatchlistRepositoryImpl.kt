package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.WatchlistDao
import com.jiucaihua.app.data.local.entity.WatchlistEntity
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.WatchlistItem
import com.jiucaihua.app.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
) : WatchlistRepository {

    override fun getAllWatchlist(): Flow<List<WatchlistItem>> {
        return watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addWatchlistItem(code: String, name: String, marketType: MarketType): Long {
        return watchlistDao.insert(
            WatchlistEntity(code = code, name = name, marketType = marketType.name)
        )
    }

    override suspend fun removeWatchlistItem(id: Long) {
        watchlistDao.deleteById(id)
    }

    override suspend fun isWatched(code: String): Boolean {
        return watchlistDao.getWatchlistByCode(code) != null
    }

    private fun WatchlistEntity.toDomain(): WatchlistItem {
        return WatchlistItem(
            id = id,
            code = code,
            name = name,
            marketType = MarketType.valueOf(marketType),
        )
    }
}
