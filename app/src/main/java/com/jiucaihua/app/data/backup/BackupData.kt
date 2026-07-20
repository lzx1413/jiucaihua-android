package com.jiucaihua.app.data.backup

import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.AlertRecordEntity
import com.jiucaihua.app.data.local.entity.FundCacheEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.data.local.entity.HoldingSnapshotEntity
import com.jiucaihua.app.data.local.entity.NewsFlashEntity
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import com.jiucaihua.app.data.local.entity.StockCacheEntity
import com.jiucaihua.app.data.local.entity.TransactionEntity
import com.jiucaihua.app.data.local.entity.WatchlistEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = BACKUP_VERSION,
    val exportTime: Long,
    val holdings: List<HoldingEntity>,
    val stockCache: List<StockCacheEntity> = emptyList(),
    val fundCache: List<FundCacheEntity> = emptyList(),
    val alerts: List<AlertEntity>,
    val alertRecords: List<AlertRecordEntity> = emptyList(),
    val settings: AppSettingsBackup,
    val portfolioSnapshots: List<PortfolioSnapshotEntity> = emptyList(),
    val holdingSnapshots: List<HoldingSnapshotEntity> = emptyList(),
    val watchlistItems: List<WatchlistEntity> = emptyList(),
    val newsFlash: List<NewsFlashEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
) {
    companion object {
        const val BACKUP_VERSION = 8
    }
}

@Serializable
data class AppSettingsBackup(
    val refreshIntervalSeconds: Int = 10,
    val isDarkMode: Boolean? = null,
    val languageTag: String = "",
    val oledMode: Boolean = false,
    val alertsEnabled: Boolean = true,
    val cash: Float = 0f,
    val lossCompensation: Float = 0f,
)

@Serializable
data class RestoreResult(
    val holdingsCount: Int,
    val alertsCount: Int,
    val alertRecordsCount: Int = 0,
    val watchlistCount: Int = 0,
    val newsFlashCount: Int = 0,
    val portfolioSnapshotsCount: Int = 0,
    val stockCacheCount: Int = 0,
    val fundCacheCount: Int = 0,
    val transactionsCount: Int = 0,
    val settingsRestored: Boolean,
)
