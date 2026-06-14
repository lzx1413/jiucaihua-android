package com.jiucaihua.app.data.backup

import com.jiucaihua.app.data.local.entity.AlertEntity
import com.jiucaihua.app.data.local.entity.HoldingEntity
import com.jiucaihua.app.data.local.entity.PortfolioSnapshotEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = BACKUP_VERSION,
    val exportTime: Long,
    val holdings: List<HoldingEntity>,
    val alerts: List<AlertEntity>,
    val settings: AppSettingsBackup,
    val portfolioSnapshots: List<PortfolioSnapshotEntity> = emptyList(),
) {
    companion object {
        const val BACKUP_VERSION = 3
    }
}

@Serializable
data class AppSettingsBackup(
    val refreshIntervalSeconds: Int = 10,
    val isDarkMode: Boolean? = null,
    val alertsEnabled: Boolean = true,
    val cash: Float = 0f,
    val lossCompensation: Float = 0f,
)

@Serializable
data class RestoreResult(
    val holdingsCount: Int,
    val alertsCount: Int,
    val settingsRestored: Boolean,
)