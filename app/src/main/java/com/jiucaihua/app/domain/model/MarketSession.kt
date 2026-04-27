package com.jiucaihua.app.domain.model

enum class MarketSession(val label: String) {
    TRADING("交易中"),
    LUNCH_BREAK("午休"),
    CLOSED("已收盘"),
    HOLIDAY("休市"),
}
