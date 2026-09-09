package com.jiucaihua.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jiucaihua.app.data.local.converter.Converters
import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.AlertRecordDao
import com.jiucaihua.app.data.local.dao.FundCacheDao
import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.dao.HoldingSnapshotDao
import com.jiucaihua.app.data.local.dao.KLineCacheDao
import com.jiucaihua.app.data.local.dao.NewsFlashDao
import com.jiucaihua.app.data.local.dao.PortfolioSnapshotDao
import com.jiucaihua.app.data.local.dao.SecurityEventDao
import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.dao.TransactionDao
import com.jiucaihua.app.data.local.dao.TransactionLotMatchDao
import com.jiucaihua.app.data.local.dao.WatchlistDao
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.AlertRecordEntity
import com.jiucaihua.app.data.local.entity.FundCacheEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.data.local.entity.HoldingSnapshotEntity
import com.jiucaihua.app.data.local.entity.KLineCacheEntity
import com.jiucaihua.app.data.local.entity.NewsFlashEntity
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import com.jiucaihua.app.data.local.entity.SecurityEventEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSymbolEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSyncStateEntity
import com.jiucaihua.app.data.local.entity.SecurityEventReadEntity
import com.jiucaihua.app.data.local.entity.SecurityEventDeliveryEntity
import com.jiucaihua.app.data.local.entity.StockCacheEntity
import com.jiucaihua.app.data.local.entity.TransactionEntity
import com.jiucaihua.app.data.local.entity.TransactionLotMatchEntity
import com.jiucaihua.app.data.local.entity.WatchlistEntity

@Database(
    entities = [HoldingEntity::class, HoldingSnapshotEntity::class, StockCacheEntity::class, FundCacheEntity::class, AlertEntity::class, AlertRecordEntity::class, WatchlistEntity::class, NewsFlashEntity::class, PortfolioSnapshotEntity::class, TransactionEntity::class, TransactionLotMatchEntity::class, SecurityEventEntity::class, SecurityEventSymbolEntity::class, SecurityEventSyncStateEntity::class, SecurityEventReadEntity::class, SecurityEventDeliveryEntity::class, KLineCacheEntity::class],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun holdingSnapshotDao(): HoldingSnapshotDao
    abstract fun stockCacheDao(): StockCacheDao
    abstract fun fundCacheDao(): FundCacheDao
    abstract fun alertDao(): AlertDao
    abstract fun alertRecordDao(): AlertRecordDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun newsFlashDao(): NewsFlashDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLotMatchDao(): TransactionLotMatchDao
    abstract fun securityEventDao(): SecurityEventDao
    abstract fun kLineCacheDao(): KLineCacheDao
}
