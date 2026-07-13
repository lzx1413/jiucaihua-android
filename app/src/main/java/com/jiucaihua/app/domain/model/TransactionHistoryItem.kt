package com.jiucaihua.app.domain.model

data class TransactionHistoryItem(
    val transaction: InvestmentTransaction,
    val proceedsCny: Double = 0.0,
    val costBasisCny: Double = 0.0,
    val realizedPnlCny: Double = 0.0,
    val dividendIncomeCny: Double = 0.0,
    val unmatchedQuantity: Double = 0.0,
)
