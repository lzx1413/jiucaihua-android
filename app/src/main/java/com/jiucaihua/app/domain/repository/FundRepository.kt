package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod

interface FundRepository {
    suspend fun getFundQuotes(codes: List<String>): List<FundQuote>
    suspend fun getCachedFundQuotes(codes: List<String>): List<FundQuote>
    suspend fun getFundNavHistory(code: String, limit: Int = 120): KLineData
}
