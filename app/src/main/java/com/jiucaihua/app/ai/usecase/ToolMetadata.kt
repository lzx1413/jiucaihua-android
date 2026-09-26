package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.domain.model.MarketType

internal fun toolCurrency(code: String): String = when (MarketType.fromCode(code)) {
    MarketType.HK_STOCK -> "HKD"
    MarketType.US_STOCK -> "USD"
    MarketType.GOLD -> if (code.startsWith("hf_")) "USD" else "CNY"
    else -> "CNY"
}
