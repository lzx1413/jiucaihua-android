package com.jiucaihua.app.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchlistSnapshot(
    val items: List<WatchlistItemSnapshot>,
    val generatedAt: String,
)

@Serializable
data class WatchlistItemSnapshot(
    val code: String,
    val name: String,
    val marketType: String,
    val currentPrice: Double? = null,
    val changePercent: Double? = null,
    val changeAmount: Double? = null,
    val quoteStatus: String = "ok",
    val sourceTime: String? = null,
    val currency: String? = null,
)
