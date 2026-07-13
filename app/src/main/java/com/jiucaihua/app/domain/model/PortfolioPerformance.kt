package com.jiucaihua.app.domain.model

data class PortfolioPerformance(
    val startDate: Long,
    val endDate: Long,
    val startValue: Double,
    val endValue: Double,
    val externalCashIn: Double,
    val externalCashOut: Double,
    val netExternalFlow: Double,
    val absoluteReturn: Double,
    val twr: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double,
    val dividendIncome: Double,
    val fees: Double,
    val taxes: Double,
    val cash: Double,
    val holdingsMarketValue: Double,
)
