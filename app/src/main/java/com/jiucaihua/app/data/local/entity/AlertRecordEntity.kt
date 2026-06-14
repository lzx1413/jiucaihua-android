package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_records")
data class AlertRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertId: Long,
    val code: String,
    val name: String,
    val alertType: String,
    val threshold: Double,
    val currentValue: Double,
    val actionHint: String? = null,
    val params: String = "{}",
    val triggeredAt: Long = System.currentTimeMillis(),
)
