package com.jiucaihua.app.domain.model

data class TransactionQuery(
    val code: String? = null,
    val marketType: MarketType? = null,
    val type: TransactionType? = null,
    val from: Long? = null,
    val to: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)
