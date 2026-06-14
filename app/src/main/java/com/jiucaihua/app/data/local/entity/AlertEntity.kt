package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val alertType: String,
    val threshold: Double,
    val actionHint: String? = null,
    val params: String = "{}",
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
