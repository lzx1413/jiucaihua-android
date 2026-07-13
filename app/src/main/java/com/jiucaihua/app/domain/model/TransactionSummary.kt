package com.jiucaihua.app.domain.model

data class TransactionSummary(
    val buyAmountCny: Double = 0.0,
    val sellAmountCny: Double = 0.0,
    val netInvestmentCny: Double = 0.0,
    val realizedPnlCny: Double = 0.0,
    val dividendIncomeCny: Double = 0.0,
    val feesCny: Double = 0.0,
    val taxesCny: Double = 0.0,
    val cashInCny: Double = 0.0,
    val cashOutCny: Double = 0.0,
    val tradeCount: Int = 0,
    val buyCount: Int = 0,
    val sellCount: Int = 0,
    val firstTradeDate: Long? = null,
    val lastTradeDate: Long? = null,
)
