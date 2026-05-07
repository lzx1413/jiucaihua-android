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
    val currentPrice: Double,
    val changePercent: Double,
    val changeAmount: Double,
)
