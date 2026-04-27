package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fund_cache")
data class FundCacheEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val estimatedValue: Double,
    val dailyChangePercent: Double,
    val netAssetValue: Double,
    val estimateTime: String,
    val navDate: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
