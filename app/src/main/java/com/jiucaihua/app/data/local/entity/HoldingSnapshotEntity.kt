package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "holding_snapshots",
    indices = [Index(value = ["code", "marketType", "date"], unique = true), Index(value = ["date"])],
)
data class HoldingSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val marketType: String,
    val date: String,
    val timestamp: Long,
    val holdingShares: Double,
    val currentPrice: Double,
    val costPrice: Double,
    val exchangeRate: Double,
    val marketValueCny: Double,
    val costCny: Double,
    val dailyEarningsCny: Double,
)
