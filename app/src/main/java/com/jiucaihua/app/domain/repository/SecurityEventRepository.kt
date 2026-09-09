package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.SecurityEvent
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.StockFundFlowSnapshot

interface SecurityEventRepository {
    suspend fun getEvents(
        code: SecurityId,
        kinds: Set<SecurityEventKind> = SecurityEventKind.entries.toSet(),
        limit: Int = 10,
        refresh: Boolean = true,
    ): List<SecurityEvent>

    suspend fun syncEvents(code: SecurityId, kinds: Set<SecurityEventKind> = SecurityEventKind.entries.toSet()): List<SecurityEvent>

    suspend fun getStockFundFlow(code: SecurityId, refresh: Boolean = true): StockFundFlowSnapshot?
    suspend fun markEventRead(event: SecurityEvent)
    suspend fun hasSuccessfulSync(code: SecurityId): Boolean
    suspend fun isEventNotified(event: SecurityEvent): Boolean
    suspend fun markEventNotified(event: SecurityEvent, symbol: SecurityId)
    suspend fun countEventNotifications(code: SecurityId, since: Long): Int
}
