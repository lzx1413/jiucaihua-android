package com.jiucaihua.app.domain.model

data class InvestmentTransaction(
    val id: Long = 0,
    val code: String? = null,
    val name: String? = null,
    val marketType: MarketType? = null,
    val type: TransactionType,
    val tradeDate: Long,
    val quantity: Double = 0.0,
    val price: Double = 0.0,
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val tax: Double = 0.0,
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val amountCny: Double
        get() = amount * exchangeRate

    val feeCny: Double
        get() = fee * exchangeRate

    val taxCny: Double
        get() = tax * exchangeRate
}
