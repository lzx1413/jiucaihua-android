package com.jiucaihua.app.domain.model

enum class MarketType(val label: String) {
    A_STOCK("A股"),
    HK_STOCK("港股"),
    FUND("基金");

    companion object {
        fun fromCode(code: String): MarketType {
            return when {
                code.startsWith("sh") || code.startsWith("sz") || code.startsWith("bj") -> A_STOCK
                code.startsWith("hk") -> HK_STOCK
                code.all { it.isDigit() } -> FUND
                else -> A_STOCK
            }
        }
    }
}
