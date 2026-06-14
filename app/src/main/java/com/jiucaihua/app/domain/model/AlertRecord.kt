package com.jiucaihua.app.domain.model

data class AlertRecord(
    val id: Long = 0,
    val alertId: Long,
    val code: String,
    val name: String,
    val alertType: AlertType,
    val threshold: Double,
    val currentValue: Double,
    val actionHint: String? = null,
    val params: Map<String, String> = emptyMap(),
    val triggeredAt: Long = System.currentTimeMillis(),
)
