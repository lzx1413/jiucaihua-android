package com.jiucaihua.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jiucaihua.app.data.local.converter.Converters
import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.FundCacheDao
import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.FundCacheEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.data.local.entity.StockCacheEntity

@Database(
    entities = [HoldingEntity::class, StockCacheEntity::class, FundCacheEntity::class, AlertEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun stockCacheDao(): StockCacheDao
    abstract fun fundCacheDao(): FundCacheDao
    abstract fun alertDao(): AlertDao
}
