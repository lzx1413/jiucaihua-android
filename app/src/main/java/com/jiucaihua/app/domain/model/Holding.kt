package com.jiucaihua.app.domain.model

data class Holding(
    val id: Long = 0,
    val code: String,
    val name: String,
    val marketType: MarketType,
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0,
    val costPrice: Double,
    val holdingAmount: Double,
    val holdingShares: Double,
    val currentPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val isSoldOut: Boolean = false,
) {
    val dailyEarnings: Double
        get() = marketValue * changePercent / 100

    val dailyEarningsCNY: Double
        get() = dailyEarnings * exchangeRate

    val earnings: Double
        get() = if (marketType == MarketType.FUND) {
            if (costPrice > 0) (currentPrice / costPrice) * holdingAmount - holdingAmount else 0.0
        } else {
            (currentPrice - costPrice) * holdingShares
        }

    val earningsCNY: Double
        get() = earnings * exchangeRate

    val earningsPercent: Double
        get() = if (costPrice > 0) (currentPrice - costPrice) / costPrice * 100 else 0.0

    val marketValue: Double
        get() = if (marketType == MarketType.FUND) {
            currentPrice * holdingShares
        } else {
            currentPrice * holdingShares
        }

    val marketValueCNY: Double
        get() = marketValue * exchangeRate
}
