package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "portfolio_snapshots", indices = [Index(value = ["date"], unique = true)])
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val timestamp: Long,
    val totalMarketValue: Double,
    val totalCost: Double,
    val totalEarnings: Double,
    val totalEarningsPercent: Double,
    val todayEarnings: Double,
    val cash: Double,
    val lossCompensation: Double,
    val categoryValuesJson: String,
    val benchmarkValue: Double = 0.0,
)
