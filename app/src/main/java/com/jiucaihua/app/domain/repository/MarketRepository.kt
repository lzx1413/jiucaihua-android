package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.FundFlowData
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketIndex

interface MarketRepository {
    suspend fun getAStockIndices(): List<MarketIndex>
    suspend fun getHKStockIndices(): List<MarketIndex>
    suspend fun getUSStockIndices(): List<MarketIndex>
    suspend fun getGoldIndices(): List<MarketIndex>
    suspend fun getFundFlowData(): FundFlowData
    suspend fun getIndexKLineData(code: String, period: KLinePeriod, limit: Int = 120): KLineData
}