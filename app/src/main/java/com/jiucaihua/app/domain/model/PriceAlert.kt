package com.jiucaihua.app.domain.model

data class PriceAlert(
    val id: Long = 0,
    val code: String,
    val name: String,
    val alertType: AlertType,
    val threshold: Double,
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class AlertType(val label: String) {
    PRICE_ABOVE("价格高于"),
    PRICE_BELOW("价格低于"),
    CHANGE_ABOVE("涨幅超过"),
    CHANGE_BELOW("跌幅超过");
}
