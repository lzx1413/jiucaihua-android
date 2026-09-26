package com.jiucaihua.app.ai.model

import com.jiucaihua.app.domain.model.FundFlowData
import com.jiucaihua.app.domain.model.MarketIndex
import com.jiucaihua.app.domain.model.NorthFlowData
import com.jiucaihua.app.domain.model.SouthFlowData

data class MarketIndicesSnapshot(
    val generatedAt: String,
    val groups: List<MarketIndexGroup>,
)

data class MarketIndexGroup(
    val market: String,
    val label: String,
    val indices: List<MarketIndexToolItem>,
)

data class FundFlowSnapshot(
    val generatedAt: String,
    val updateTime: String?,
    val northFlow: NorthFlowData,
    val southFlow: SouthFlowData,
    val unit: String = "10000_provider_currency",
    val currency: String = "provider_native",
)

data class SearchResultsSnapshot(
    val generatedAt: String,
    val keyword: String,
    val total: Int,
    val results: List<SearchResultEntry>,
    val limit: Int,
    val possiblyTruncated: Boolean,
)

data class SearchResultEntry(
    val code: String,
    val displayCode: String,
    val name: String,
    val marketType: String,
)

data class MarketStatusSnapshot(
    val generatedAt: String,
    val isTodayHoliday: Boolean,
    val sessions: Map<String, String>,
    val hkdToCnyRate: Double,
)

data class MarketIndexToolItem(
    val code: String,
    val name: String,
    val price: Double?,
    val changePercent: Double?,
    val changeAmount: Double?,
    val sourceTime: String?,
    val priceUnit: String,
    val currency: String?,
    val status: String,
)
