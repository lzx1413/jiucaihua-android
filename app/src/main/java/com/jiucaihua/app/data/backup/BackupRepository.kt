package com.jiucaihua.app.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.AlertRecordDao
import com.jiucaihua.app.data.local.dao.FundCacheDao
import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.dao.NewsFlashDao
import com.jiucaihua.app.data.local.dao.PortfolioSnapshotDao
import com.jiucaihua.app.data.local.dao.StockCacheDao
import com.jiucaihua.app.data.local.dao.TransactionDao
import com.jiucaihua.app.data.local.dao.TransactionLotMatchDao
import com.jiucaihua.app.data.local.dao.WatchlistDao
import com.jiucaihua.app.data.local.AppDatabase
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.data.local.entity.TransactionEntity
import com.jiucaihua.app.presentation.settings.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val holdingDao: HoldingDao,
    private val stockCacheDao: StockCacheDao,
    private val fundCacheDao: FundCacheDao,
    private val alertDao: AlertDao,
    private val alertRecordDao: AlertRecordDao,
    private val snapshotDao: PortfolioSnapshotDao,
    private val watchlistDao: WatchlistDao,
    private val newsFlashDao: NewsFlashDao,
    private val transactionDao: TransactionDao,
    private val transactionLotMatchDao: TransactionLotMatchDao,
    @param:Named("appPrefs") private val prefs: SharedPreferences,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun exportData(): BackupData = withContext(Dispatchers.IO) {
        val holdings = holdingDao.getAllHoldingsOnce()
        val stockCache = stockCacheDao.getAllOnce()
        val fundCache = fundCacheDao.getAllOnce()
        val alerts = alertDao.getAllAlertsOnce()
        val alertRecords = alertRecordDao.getAllRecordsOnce()
        val snapshots = snapshotDao.getAllOnce()
        val watchlistItems = watchlistDao.getAllWatchlistOnce()
        val newsFlash = newsFlashDao.getAllOnce()
        val transactions = transactionDao.getAllOnce()
        val settings = AppSettingsBackup(
            refreshIntervalSeconds = prefs.getInt(SettingsViewModel.KEY_REFRESH_INTERVAL, 10),
            isDarkMode = if (prefs.contains(SettingsViewModel.KEY_DARK_MODE)) {
                prefs.getBoolean(SettingsViewModel.KEY_DARK_MODE, false)
            } else null,
            languageTag = prefs.getString(SettingsViewModel.KEY_LANGUAGE, "").orEmpty(),
            oledMode = prefs.getBoolean(SettingsViewModel.KEY_OLED_MODE, false),
            alertsEnabled = prefs.getBoolean(SettingsViewModel.KEY_ALERTS_ENABLED, true),
            cash = prefs.getFloat("cash", 0f),
            lossCompensation = prefs.getFloat("loss_compensation", 0f),
        )
        BackupData(
            exportTime = System.currentTimeMillis(),
            holdings = holdings,
            stockCache = stockCache,
            fundCache = fundCache,
            alerts = alerts,
            alertRecords = alertRecords,
            settings = settings,
            portfolioSnapshots = snapshots,
            watchlistItems = watchlistItems,
            newsFlash = newsFlash,
            transactions = transactions,
        )
    }

    fun serializeBackup(data: BackupData): String {
        return json.encodeToString(data)
    }

    fun deserializeBackup(content: String): BackupData {
        return json.decodeFromString<BackupData>(content)
    }

    fun getDefaultFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "jiucaihua_backup_$timestamp.json"
    }

    suspend fun restoreData(
        backup: BackupData,
        mode: RestoreMode = RestoreMode.REPLACE,
    ): RestoreResult = withContext(Dispatchers.IO) {
        val result = database.withTransaction {
            when (mode) {
                RestoreMode.REPLACE -> restoreReplace(backup)
                RestoreMode.MERGE -> restoreMerge(backup)
            }
        }

        prefs.edit().apply {
            putInt(SettingsViewModel.KEY_REFRESH_INTERVAL, backup.settings.refreshIntervalSeconds)
            if (backup.settings.isDarkMode != null) {
                putBoolean(SettingsViewModel.KEY_DARK_MODE, backup.settings.isDarkMode)
            } else {
                remove(SettingsViewModel.KEY_DARK_MODE)
            }
            if (backup.settings.languageTag.isBlank()) {
                remove(SettingsViewModel.KEY_LANGUAGE)
            } else {
                putString(SettingsViewModel.KEY_LANGUAGE, backup.settings.languageTag)
            }
            putBoolean(SettingsViewModel.KEY_OLED_MODE, backup.settings.oledMode)
            putBoolean(SettingsViewModel.KEY_ALERTS_ENABLED, backup.settings.alertsEnabled)
            putFloat("cash", backup.settings.cash)
            putFloat("loss_compensation", backup.settings.lossCompensation)
            apply()
        }

        result.copy(settingsRestored = true)
    }

    private suspend fun restoreReplace(backup: BackupData): RestoreResult {
        holdingDao.clearAllHoldings()
        stockCacheDao.clearAll()
        fundCacheDao.clearAll()
        alertDao.clearAllAlerts()
        alertRecordDao.clearAllRecords()
        watchlistDao.clearAll()
        newsFlashDao.clearAll()
        snapshotDao.clearAll()
        transactionDao.clearAll()
        transactionLotMatchDao.clearAll()

        val holdings = backup.holdings.map { it.copy(id = 0) }
        val alerts = backup.alerts.map { it.copy(id = 0) }
        val alertRecords = backup.alertRecords.map { it.copy(id = 0) }
        val watchlistItems = backup.watchlistItems.map { it.copy(id = 0) }
        val newsFlash = backup.newsFlash.map { it.copy(id = 0) }
        val snapshots = backup.portfolioSnapshots.map { it.copy(id = 0) }
        val transactions = restoredTransactions(backup.transactions, holdings)

        holdingDao.insertAllHoldings(holdings)
        stockCacheDao.insertAll(backup.stockCache)
        fundCacheDao.insertAll(backup.fundCache)
        alertDao.insertAllAlerts(alerts)
        alertRecordDao.insertAll(alertRecords)
        watchlistDao.insertAll(watchlistItems)
        newsFlashDao.insertAll(newsFlash)
        snapshotDao.insertAll(snapshots)
        transactionDao.insertAll(transactions)

        return RestoreResult(
            holdingsCount = holdings.size,
            alertsCount = alerts.size,
            alertRecordsCount = alertRecords.size,
            watchlistCount = watchlistItems.size,
            newsFlashCount = newsFlash.size,
            portfolioSnapshotsCount = snapshots.size,
            stockCacheCount = backup.stockCache.size,
            fundCacheCount = backup.fundCache.size,
            transactionsCount = transactions.size,
            settingsRestored = false,
        )
    }

    private suspend fun restoreMerge(backup: BackupData): RestoreResult {
        val existingHoldingKeys = holdingDao.getAllHoldingsOnce()
            .map { holdingKey(it) }
            .toSet()
        val newHoldings = backup.holdings
            .filter { holdingKey(it) !in existingHoldingKeys }
            .map { it.copy(id = 0) }
        holdingDao.insertAllHoldings(newHoldings)

        stockCacheDao.insertAll(backup.stockCache)
        fundCacheDao.insertAll(backup.fundCache)

        val existingAlertKeys = alertDao.getAllAlertsOnce()
            .map { alertKey(it) }
            .toSet()
        val newAlerts = backup.alerts
            .filter { alertKey(it) !in existingAlertKeys }
            .map { it.copy(id = 0) }
        alertDao.insertAllAlerts(newAlerts)

        val existingAlertRecordKeys = alertRecordDao.getAllRecordsOnce()
            .map { "${it.code}:${it.alertType}:${it.threshold}:${it.currentValue}:${it.triggeredAt}" }
            .toSet()
        val newAlertRecords = backup.alertRecords
            .filter { "${it.code}:${it.alertType}:${it.threshold}:${it.currentValue}:${it.triggeredAt}" !in existingAlertRecordKeys }
            .map { it.copy(id = 0) }
        alertRecordDao.insertAll(newAlertRecords)

        val existingWatchlistCodes = watchlistDao.getAllWatchlistOnce().map { it.code }.toSet()
        val newWatchlistItems = backup.watchlistItems
            .filter { it.code !in existingWatchlistCodes }
            .map { it.copy(id = 0) }
        watchlistDao.insertAll(newWatchlistItems)

        val existingNewsKeys = newsFlashDao.getAllOnce()
            .map { "${it.newsId}:${it.sourceType}" }
            .toSet()
        val newNewsFlash = backup.newsFlash
            .filter { "${it.newsId}:${it.sourceType}" !in existingNewsKeys }
            .map { it.copy(id = 0) }
        newsFlashDao.insertAll(newNewsFlash)

        val existingSnapshotDates = snapshotDao.getAllOnce().map { it.date }.toSet()
        val newSnapshots = backup.portfolioSnapshots
            .filter { it.date !in existingSnapshotDates }
            .map { it.copy(id = 0) }
        snapshotDao.insertAll(newSnapshots)

        val existingTransactionKeys = transactionDao.getAllOnce()
            .map { transactionKey(it) }
            .toSet()
        val restoredBackupTransactions = restoredTransactions(backup.transactions, backup.holdings)
        val newTransactions = restoredBackupTransactions
            .filter { transactionKey(it) !in existingTransactionKeys }
            .map { it.copy(id = 0) }
        transactionDao.insertAll(newTransactions)

        return RestoreResult(
            holdingsCount = newHoldings.size,
            alertsCount = newAlerts.size,
            alertRecordsCount = newAlertRecords.size,
            watchlistCount = newWatchlistItems.size,
            newsFlashCount = newNewsFlash.size,
            portfolioSnapshotsCount = newSnapshots.size,
            stockCacheCount = backup.stockCache.size,
            fundCacheCount = backup.fundCache.size,
            transactionsCount = newTransactions.size,
            settingsRestored = false,
        )
    }

    private fun alertKey(alert: AlertEntity): String {
        return "${alert.code}:${alert.alertType}:${alert.threshold}:${alert.params}:${alert.actionHint.orEmpty()}"
    }

    private fun holdingKey(holding: HoldingEntity): String {
        return "${holding.code}:${holding.marketType}:${holding.createdAt}:${holding.updatedAt}:${holding.isSoldOut}"
    }

    private fun transactionKey(transaction: com.jiucaihua.app.data.local.entity.TransactionEntity): String {
        return "${transaction.code}:${transaction.marketType}:${transaction.type}:${transaction.tradeDate}:${transaction.quantity}:${transaction.price}:${transaction.amount}:${transaction.createdAt}"
    }

    private fun restoredTransactions(
        backupTransactions: List<TransactionEntity>,
        restoredHoldings: List<HoldingEntity>,
    ): List<TransactionEntity> {
        if (backupTransactions.isNotEmpty()) {
            return backupTransactions.map { it.copy(id = 0) }
        }
        return restoredHoldings
            .filter { !it.isSoldOut && it.holdingShares > 0.0 }
            .map { holding ->
                TransactionEntity(
                    code = holding.code,
                    name = holding.name,
                    marketType = holding.marketType,
                    type = "BUY",
                    tradeDate = holding.createdAt,
                    quantity = holding.holdingShares,
                    price = holding.costPrice,
                    amount = holding.holdingAmount,
                    currency = holding.currency,
                    exchangeRate = legacyExchangeRate(holding.marketType),
                    note = LEGACY_HOLDING_TRANSACTION_NOTE,
                    createdAt = holding.createdAt,
                    updatedAt = holding.updatedAt,
                )
            }
    }

    private fun legacyExchangeRate(marketType: String): Double {
        return when (marketType) {
            "HK_STOCK" -> 0.92
            "US_STOCK" -> 7.2
            else -> 1.0
        }
    }

    private companion object {
        const val LEGACY_HOLDING_TRANSACTION_NOTE = "备份恢复初始化持仓"
    }

    suspend fun writeToUri(outputStream: OutputStream, content: String) = withContext(Dispatchers.IO) {
        outputStream.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    enum class RestoreMode {
        REPLACE,
        MERGE,
    }
}
