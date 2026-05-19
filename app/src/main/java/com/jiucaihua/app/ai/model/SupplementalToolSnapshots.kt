package com.jiucaihua.app.ai.model

import com.jiucaihua.app.domain.model.KLinePeriod

data class KLineToolSnapshot(
    val code: String,
    val name: String,
    val period: KLinePeriod,
    val pointsCount: Int,
    val latestPoint: KLinePointSnapshot?,
    val highestHigh: Double,
    val lowestLow: Double,
    val points: List<KLinePointSnapshot>,
)

data class KLinePointSnapshot(
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val changePercent: Double,
    val ma5: Double? = null,
    val ma20: Double? = null,
    val ma60: Double? = null,
    val volumeRatio: Double? = null,
    val dif: Double? = null,
    val dea: Double? = null,
    val macd: Double? = null,
    val rsi6: Double? = null,
    val rsi12: Double? = null,
    val rsi24: Double? = null,
    val bollUpper: Double? = null,
    val bollMiddle: Double? = null,
    val bollLower: Double? = null,
)

data class MarketNewsDigest(
    val generatedAt: String,
    val total: Int,
    val items: List<NewsSnapshot>,
)

data class AlertsToolSnapshot(
    val total: Int,
    val enabledCount: Int,
    val recentTriggeredCount: Int,
    val alerts: List<AlertSnapshot>,
)

data class WhatIfAnalysisSnapshot(
    val code: String,
    val name: String,
    val positionUnits: Double,
    val unitLabel: String,
    val currentPrice: Double,
    val targetPrice: Double,
    val targetChangePercent: Double,
    val costBasisCny: Double,
    val currentMarketValueCny: Double,
    val targetMarketValueCny: Double,
    val currentUnrealizedPnlCny: Double,
    val targetUnrealizedPnlCny: Double,
    val pnlDifferenceCny: Double,
    val currentUnrealizedPnlPercent: Double,
    val targetUnrealizedPnlPercent: Double,
)
