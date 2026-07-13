package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionLotMatch

interface TransactionLotMatchRepository {
    suspend fun getBySecurity(code: String, marketType: MarketType): List<TransactionLotMatch>
    suspend fun getBySellTransactionIds(sellTransactionIds: List<Long>): List<TransactionLotMatch>
    suspend fun replaceForSecurity(code: String, marketType: MarketType, matches: List<TransactionLotMatch>)
    suspend fun clearAll()
}
