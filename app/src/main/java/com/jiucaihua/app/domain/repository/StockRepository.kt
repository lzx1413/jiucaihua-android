package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.StockQuote

interface StockRepository {
    suspend fun getAStockQuotes(codes: List<String>): List<StockQuote>
    suspend fun getHKStockQuotes(codes: List<String>): List<StockQuote>
    suspend fun getCachedQuotes(codes: List<String>): List<StockQuote>
    suspend fun getKLineData(code: String, period: KLinePeriod, limit: Int = 120): KLineData
}
