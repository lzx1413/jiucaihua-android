package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.SecurityEventDao
import com.jiucaihua.app.data.local.entity.SecurityEventEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSymbolEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSyncStateEntity
import com.jiucaihua.app.data.local.entity.SecurityEventReadEntity
import com.jiucaihua.app.data.local.entity.SecurityEventDeliveryEntity
import com.jiucaihua.app.data.remote.datasource.TencentRemoteDataSource
import com.jiucaihua.app.domain.model.DataProvider
import com.jiucaihua.app.domain.model.DataServedFrom
import com.jiucaihua.app.domain.model.SecurityEvent
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.StockFundFlowSnapshot
import com.jiucaihua.app.domain.repository.SecurityEventRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityEventRepositoryImpl @Inject constructor(
    private val remoteDataSource: TencentRemoteDataSource,
    private val eventDao: SecurityEventDao,
) : SecurityEventRepository {
    override suspend fun getEvents(
        code: SecurityId,
        kinds: Set<SecurityEventKind>,
        limit: Int,
        refresh: Boolean,
    ): List<SecurityEvent> {
        val boundedLimit = limit.coerceIn(1, MAX_EVENT_LIMIT)
        if (refresh) {
            try {
                syncEvents(code, kinds)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Cached events remain useful as an explicitly stale result.
            }
        }
        return eventDao.getForSymbol(code.value, kinds.map { it.name }, boundedLimit)
            .map { it.toDomain(eventDao) }
    }

    override suspend fun syncEvents(code: SecurityId, kinds: Set<SecurityEventKind>): List<SecurityEvent> {
        val fetchedByKind = linkedMapOf<SecurityEventKind, List<SecurityEvent>>()
        var lastFailure: Throwable? = null
        kinds.forEach { kind ->
            try {
                fetchedByKind[kind] = fetchIncrementally(code, kind)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastFailure = error
            }
        }
        if (fetchedByKind.isEmpty()) throw (lastFailure ?: IllegalStateException("PROVIDER_UNAVAILABLE"))
        val events = fetchedByKind.values.flatten().distinctBy { "${it.provider}:${it.kind}:${it.externalId}" }
        persist(events, code, fetchedByKind.keys)
        return events
    }

    override suspend fun getStockFundFlow(code: SecurityId, refresh: Boolean): StockFundFlowSnapshot? {
        if (!refresh) return null
        return remoteDataSource.getStockFundFlow(code)
    }

    override suspend fun markEventRead(event: SecurityEvent) {
        eventDao.markRead(
            SecurityEventReadEntity(
                provider = event.provider.name,
                kind = event.kind.name,
                externalId = event.externalId,
                readAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun hasSuccessfulSync(code: SecurityId): Boolean =
        eventDao.countSyncStates(DataProvider.TENCENT.name, code.value) > 0

    override suspend fun isEventNotified(event: SecurityEvent): Boolean =
        eventDao.isDelivered(event.provider.name, event.kind.name, event.externalId)

    override suspend fun markEventNotified(event: SecurityEvent, symbol: SecurityId) {
        eventDao.recordDelivery(
            SecurityEventDeliveryEntity(
                provider = event.provider.name,
                kind = event.kind.name,
                externalId = event.externalId,
                symbol = symbol.value,
                deliveredAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun countEventNotifications(code: SecurityId, since: Long): Int =
        eventDao.countDeliveries(code.value, since)

    private suspend fun fetchIncrementally(code: SecurityId, kind: SecurityEventKind): List<SecurityEvent> {
        val previous = eventDao.getSyncState(DataProvider.TENCENT.name, kind.name, code.value)
        val collected = mutableListOf<SecurityEvent>()
        var page = previous?.nextPage ?: 1
        var reachedWatermark = previous == null
        for (attempt in 0 until MAX_SYNC_PAGES) {
            val pageEvents = remoteDataSource.getEvents(code, kind, page, SYNC_PAGE_SIZE)
            if (pageEvents.isEmpty()) break
            collected += pageEvents
            if (previous != null && pageEvents.any { it.publishedAt > 0 && it.publishedAt <= previous.newestPublishedAt }) {
                reachedWatermark = true
                break
            }
            if (previous == null || pageEvents.size < SYNC_PAGE_SIZE) break
            page += 1
        }
        if (previous != null && !reachedWatermark && collected.size >= MAX_SYNC_PAGES * SYNC_PAGE_SIZE) {
            throw IllegalStateException("SYNC_WINDOW_INCOMPLETE")
        }
        return collected
    }

    private suspend fun persist(
        events: List<SecurityEvent>,
        requestedCode: SecurityId,
        successfulKinds: Set<SecurityEventKind>,
    ) {
        val fetchedAt = System.currentTimeMillis()
        eventDao.upsertEventsWithSymbols(
            events = events.map { it.toEntity(fetchedAt) },
            symbols = events.flatMap { event ->
                event.symbols.ifEmpty { listOf(requestedCode) }.map { symbol ->
                    SecurityEventSymbolEntity(
                        provider = event.provider.name,
                        kind = event.kind.name,
                        externalId = event.externalId,
                        symbol = symbol.value,
                    )
                }
            },
        )
        successfulKinds.forEach { kind ->
            val previous = eventDao.getSyncState(DataProvider.TENCENT.name, kind.name, requestedCode.value)
            val newestFetched = events.filter { it.kind == kind }.maxOfOrNull { it.publishedAt } ?: 0L
            eventDao.upsertSyncState(
                SecurityEventSyncStateEntity(
                    provider = DataProvider.TENCENT.name,
                    kind = kind.name,
                    symbol = requestedCode.value,
                    lastSuccessfulSyncAt = fetchedAt,
                    newestPublishedAt = maxOf(previous?.newestPublishedAt ?: 0L, newestFetched),
                    nextPage = 1,
                )
            )
        }
    }

    private suspend fun SecurityEventEntity.toDomain(dao: SecurityEventDao): SecurityEvent = SecurityEvent(
        externalId = externalId,
        provider = DataProvider.valueOf(provider),
        kind = SecurityEventKind.valueOf(kind),
        title = title,
        summary = summary,
        contentUrl = contentUrl,
        publisher = publisher,
        publishedAt = publishedAt,
        symbols = dao.getSymbols(provider, kind, externalId).mapNotNull { SecurityId.parse(it.symbol) },
        importance = importance,
        titleMention = titleMention,
        bodyMention = bodyMention,
        researchRating = researchRating,
        reportType = reportType,
        fetchedAt = fetchedAt,
        servedFrom = DataServedFrom.ROOM_CACHE,
        isStale = System.currentTimeMillis() - fetchedAt > kind.ttlMillis(),
        isRead = dao.isRead(provider, kind, externalId),
    )

    private fun SecurityEvent.toEntity(fetchedAt: Long) = SecurityEventEntity(
        provider = provider.name,
        kind = kind.name,
        externalId = externalId,
        title = title,
        summary = summary,
        contentUrl = contentUrl,
        publisher = publisher,
        publishedAt = publishedAt,
        importance = importance,
        titleMention = titleMention,
        bodyMention = bodyMention,
        researchRating = researchRating,
        reportType = reportType,
        fetchedAt = fetchedAt,
    )

    private fun String.ttlMillis(): Long = when (SecurityEventKind.valueOf(this)) {
        SecurityEventKind.NEWS -> NEWS_TTL_MILLIS
        SecurityEventKind.ANNOUNCEMENT,
        SecurityEventKind.PERIODIC_REPORT -> ANNOUNCEMENT_TTL_MILLIS
        SecurityEventKind.RESEARCH -> RESEARCH_TTL_MILLIS
        SecurityEventKind.INDUSTRY_NEWS -> NEWS_TTL_MILLIS
    }

    private companion object {
        const val MAX_EVENT_LIMIT = 50
        const val SYNC_PAGE_SIZE = 50
        const val MAX_SYNC_PAGES = 10
        const val NEWS_TTL_MILLIS = 5 * 60 * 1000L
        const val ANNOUNCEMENT_TTL_MILLIS = 15 * 60 * 1000L
        const val RESEARCH_TTL_MILLIS = 60 * 60 * 1000L
    }
}
