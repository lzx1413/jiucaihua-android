package com.jiucaihua.app.domain.model

enum class SortOrder(val label: String) {
    DEFAULT("默认"),
    CHANGE_PERCENT_DESC("涨跌幅↓"),
    CHANGE_PERCENT_ASC("涨跌幅↑"),
    EARNINGS_DESC("盈亏额↓"),
    EARNINGS_ASC("盈亏额↑"),
    MARKET_VALUE_DESC("市值↓"),
    MARKET_VALUE_ASC("市值↑"),
}
