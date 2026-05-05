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
    val indices: List<MarketIndex>,
)

data class FundFlowSnapshot(
    val generatedAt: String,
    val updateTime: String,
    val northFlow: NorthFlowData,
    val southFlow: SouthFlowData,
)

data class SearchResultsSnapshot(
    val generatedAt: String,
    val keyword: String,
    val total: Int,
    val results: List<SearchResultEntry>,
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
