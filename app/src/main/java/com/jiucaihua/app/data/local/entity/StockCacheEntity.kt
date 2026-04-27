package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_cache")
data class StockCacheEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val currency: String = "CNY",
    val price: Double,
    val yestClose: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val amount: Double,
    val changePercent: Double,
    val changeAmount: Double,
    val time: String,
    val marketType: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
