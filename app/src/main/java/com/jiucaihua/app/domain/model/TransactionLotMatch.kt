package com.jiucaihua.app.domain.model

data class TransactionLotMatch(
    val id: Long = 0,
    val code: String,
    val marketType: MarketType,
    val sellTransactionId: Long,
    val buyTransactionId: Long,
    val quantity: Double,
    val buyUnitCostCny: Double,
    val sellUnitProceedsCny: Double,
    val costBasisCny: Double,
    val proceedsCny: Double,
    val realizedPnlCny: Double,
    val createdAt: Long = System.currentTimeMillis(),
)
