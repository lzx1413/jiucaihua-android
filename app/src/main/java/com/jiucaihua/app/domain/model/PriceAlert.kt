package com.jiucaihua.app.domain.model

data class PriceAlert(
    val id: Long = 0,
    val code: String,
    val name: String,
    val alertType: AlertType,
    val threshold: Double,
    val actionHint: String? = null,
    val params: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class AlertType(val label: String) {
    PRICE_ABOVE("价格高于"),
    PRICE_BELOW("价格低于"),
    CHANGE_ABOVE("涨幅超过"),
    CHANGE_BELOW("跌幅超过"),
    VOLUME_ABOVE("成交量高于"),
    NEW_HIGH("创N日新高"),
    NEW_LOW("创N日新低"),
    MA_CROSS_ABOVE("均线金叉"),
    MA_CROSS_BELOW("均线死叉");
}
