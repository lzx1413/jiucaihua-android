package com.jiucaihua.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jiucaihua.app.data.local.AppDatabase
import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.AlertRecordDao
import com.jiucaihua.app.data.local.dao.FundCacheDao
import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.dao.NewsFlashDao
import com.jiucaihua.app.data.local.dao.PortfolioSnapshotDao
import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.dao.TransactionDao
import com.jiucaihua.app.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jiucaihua_database"
        )
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `watchlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `marketType` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `news_flash` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `newsId` INTEGER NOT NULL, `title` TEXT NOT NULL, `summary` TEXT NOT NULL, `content` TEXT NOT NULL, `impact` TEXT NOT NULL, `source` TEXT NOT NULL, `time` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `epochMillis` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_news_flash_newsId_sourceType` ON `news_flash` (`newsId`, `sourceType`)"
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `alert_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alertId` INTEGER NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `alertType` TEXT NOT NULL, `threshold` REAL NOT NULL, `currentValue` REAL NOT NULL, `triggeredAt` INTEGER NOT NULL)"
            )
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `alerts` ADD COLUMN `actionHint` TEXT")
            db.execSQL("ALTER TABLE `alert_records` ADD COLUMN `actionHint` TEXT")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `news_flash` ADD COLUMN `detailUrl` TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portfolio_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `totalMarketValue` REAL NOT NULL, `totalCost` REAL NOT NULL, `totalEarnings` REAL NOT NULL, `totalEarningsPercent` REAL NOT NULL, `todayEarnings` REAL NOT NULL, `cash` REAL NOT NULL, `lossCompensation` REAL NOT NULL, `categoryValuesJson` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_portfolio_snapshots_date` ON `portfolio_snapshots` (`date`)"
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `alerts` ADD COLUMN `params` TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("ALTER TABLE `alert_records` ADD COLUMN `params` TEXT NOT NULL DEFAULT '{}'")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `watchlist` ADD COLUMN `group_name` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `news_flash` ADD COLUMN `isBookmarked` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `portfolio_snapshots` ADD COLUMN `benchmarkValue` REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `code` TEXT,
                    `name` TEXT,
                    `marketType` TEXT,
                    `type` TEXT NOT NULL,
                    `tradeDate` INTEGER NOT NULL,
                    `quantity` REAL NOT NULL DEFAULT 0.0,
                    `price` REAL NOT NULL DEFAULT 0.0,
                    `amount` REAL NOT NULL DEFAULT 0.0,
                    `fee` REAL NOT NULL DEFAULT 0.0,
                    `tax` REAL NOT NULL DEFAULT 0.0,
                    `currency` TEXT NOT NULL DEFAULT 'CNY',
                    `exchangeRate` REAL NOT NULL DEFAULT 1.0,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_code_marketType` ON `transactions` (`code`, `marketType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_tradeDate` ON `transactions` (`tradeDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            db.execSQL(
                """
                INSERT INTO `transactions` (
                    `code`, `name`, `marketType`, `type`, `tradeDate`, `quantity`,
                    `price`, `amount`, `fee`, `tax`, `currency`, `exchangeRate`,
                    `note`, `createdAt`, `updatedAt`
                )
                SELECT
                    `code`, `name`, `marketType`, 'BUY', `createdAt`, `holdingShares`,
                    `costPrice`, `holdingAmount`, 0.0, 0.0, `currency`,
                    CASE
                        WHEN `currency` = 'HKD' THEN 0.92
                        WHEN `currency` = 'USD' THEN 7.2
                        ELSE 1.0
                    END,
                    '初始化持仓', `createdAt`, `updatedAt`
                FROM `holdings`
                WHERE `holdingShares` > 0
                """.trimIndent()
            )
        }
    }

    @Provides
    fun provideHoldingDao(database: AppDatabase): HoldingDao {
        return database.holdingDao()
    }

    @Provides
    fun provideStockCacheDao(database: AppDatabase): StockCacheDao {
        return database.stockCacheDao()
    }

    @Provides
    fun provideFundCacheDao(database: AppDatabase): FundCacheDao {
        return database.fundCacheDao()
    }

    @Provides
    fun provideAlertDao(database: AppDatabase): AlertDao {
        return database.alertDao()
    }

    @Provides
    fun provideAlertRecordDao(database: AppDatabase): AlertRecordDao {
        return database.alertRecordDao()
    }

    @Provides
    fun provideWatchlistDao(database: AppDatabase): WatchlistDao {
        return database.watchlistDao()
    }

    @Provides
    fun provideNewsFlashDao(database: AppDatabase): NewsFlashDao {
        return database.newsFlashDao()
    }

    @Provides
    fun providePortfolioSnapshotDao(database: AppDatabase): PortfolioSnapshotDao {
        return database.portfolioSnapshotDao()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    @Named("appPrefs")
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("jiucaihua_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named("aiPrefs")
    fun provideAiSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "jiucaihua_ai_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
