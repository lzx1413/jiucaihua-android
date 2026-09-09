package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "kline_cache",
    primaryKeys = ["code", "period", "adjustment", "provider", "date"],
    indices = [Index(value = ["code", "period", "adjustment", "provider", "date"])],
)
data class KLineCacheEntity(
    val code: String,
    val period: String,
    val adjustment: String,
    val provider: String,
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val fetchedAt: Long,
)
