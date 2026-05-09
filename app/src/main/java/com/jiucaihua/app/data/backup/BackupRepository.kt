package com.jiucaihua.app.data.backup

import android.content.Context
import android.content.SharedPreferences
import com.jiucaihua.app.data.local.dao.AlertDao
import com.jiucaihua.app.data.local.dao.HoldingDao
import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
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
    @ApplicationContext private val context: Context,
    private val holdingDao: HoldingDao,
    private val alertDao: AlertDao,
    @Named("appPrefs") private val prefs: SharedPreferences,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun exportData(): BackupData = withContext(Dispatchers.IO) {
        val holdings = holdingDao.getAllHoldingsOnce()
        val alerts = alertDao.getAllAlertsOnce()
        val settings = AppSettingsBackup(
            refreshIntervalSeconds = prefs.getInt(SettingsViewModel.KEY_REFRESH_INTERVAL, 10),
            isDarkMode = if (prefs.contains(SettingsViewModel.KEY_DARK_MODE)) {
                prefs.getBoolean(SettingsViewModel.KEY_DARK_MODE, false)
            } else null,
            alertsEnabled = prefs.getBoolean(SettingsViewModel.KEY_ALERTS_ENABLED, true),
            cash = prefs.getFloat("cash", 0f),
            lossCompensation = prefs.getFloat("loss_compensation", 0f),
        )
        BackupData(
            exportTime = System.currentTimeMillis(),
            holdings = holdings,
            alerts = alerts,
            settings = settings,
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
        val holdingsInserted = when (mode) {
            RestoreMode.REPLACE -> {
                holdingDao.clearAllHoldings()
                alertDao.clearAllAlerts()
                holdingDao.insertAllHoldings(backup.holdings.map { it.copy(id = 0) })
                backup.holdings.size
            }
            RestoreMode.MERGE -> {
                val existingCodes = holdingDao.getAllHoldingsOnce().map { it.code }.toSet()
                val newHoldings = backup.holdings.filter { it.code !in existingCodes }
                holdingDao.insertAllHoldings(newHoldings.map { it.copy(id = 0) })
                newHoldings.size
            }
        }

        val alertsInserted = when (mode) {
            RestoreMode.REPLACE -> {
                alertDao.insertAllAlerts(backup.alerts.map { it.copy(id = 0) })
                backup.alerts.size
            }
            RestoreMode.MERGE -> {
                val existingAlerts = alertDao.getAllAlertsOnce()
                val existingAlertKeys = existingAlerts.map { "${it.code}:${it.alertType}:${it.threshold}" }.toSet()
                val newAlerts = backup.alerts.filter { "${it.code}:${it.alertType}:${it.threshold}" !in existingAlertKeys }
                alertDao.insertAllAlerts(newAlerts.map { it.copy(id = 0) })
                newAlerts.size
            }
        }

        prefs.edit().apply {
            putInt(SettingsViewModel.KEY_REFRESH_INTERVAL, backup.settings.refreshIntervalSeconds)
            if (backup.settings.isDarkMode != null) {
                putBoolean(SettingsViewModel.KEY_DARK_MODE, backup.settings.isDarkMode)
            } else {
                remove(SettingsViewModel.KEY_DARK_MODE)
            }
            putBoolean(SettingsViewModel.KEY_ALERTS_ENABLED, backup.settings.alertsEnabled)
            putFloat("cash", backup.settings.cash)
            putFloat("loss_compensation", backup.settings.lossCompensation)
            apply()
        }

        RestoreResult(
            holdingsCount = holdingsInserted,
            alertsCount = alertsInserted,
            settingsRestored = true,
        )
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