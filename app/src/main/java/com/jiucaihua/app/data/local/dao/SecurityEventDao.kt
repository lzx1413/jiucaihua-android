package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jiucaihua.app.data.local.entity.SecurityEventEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSymbolEntity
import com.jiucaihua.app.data.local.entity.SecurityEventSyncStateEntity
import com.jiucaihua.app.data.local.entity.SecurityEventReadEntity
import com.jiucaihua.app.data.local.entity.SecurityEventDeliveryEntity

@Dao
interface SecurityEventDao {
    @Query(
        """
        SELECT DISTINCT event.* FROM security_event AS event
        INNER JOIN security_event_symbol AS link
        ON event.provider = link.provider AND event.kind = link.kind AND event.externalId = link.externalId
        WHERE link.symbol = :symbol AND event.kind IN (:kinds)
        ORDER BY event.publishedAt DESC, event.fetchedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getForSymbol(symbol: String, kinds: List<String>, limit: Int): List<SecurityEventEntity>

    @Query(
        "SELECT * FROM security_event_symbol WHERE provider = :provider AND kind = :kind AND externalId = :externalId",
    )
    suspend fun getSymbols(provider: String, kind: String, externalId: String): List<SecurityEventSymbolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<SecurityEventEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSymbols(symbols: List<SecurityEventSymbolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: SecurityEventSyncStateEntity)

    @Query("SELECT * FROM security_event_sync_state WHERE provider = :provider AND kind = :kind AND symbol = :symbol")
    suspend fun getSyncState(provider: String, kind: String, symbol: String): SecurityEventSyncStateEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM security_event_read WHERE provider = :provider AND kind = :kind AND externalId = :externalId)")
    suspend fun isRead(provider: String, kind: String, externalId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markRead(event: SecurityEventReadEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM security_event_delivery WHERE provider = :provider AND kind = :kind AND externalId = :externalId)")
    suspend fun isDelivered(provider: String, kind: String, externalId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordDelivery(delivery: SecurityEventDeliveryEntity)

    @Query("SELECT COUNT(*) FROM security_event_delivery WHERE symbol = :symbol AND deliveredAt >= :since")
    suspend fun countDeliveries(symbol: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM security_event_sync_state WHERE provider = :provider AND symbol = :symbol")
    suspend fun countSyncStates(provider: String, symbol: String): Int

    @Transaction
    suspend fun upsertEventsWithSymbols(events: List<SecurityEventEntity>, symbols: List<SecurityEventSymbolEntity>) {
        if (events.isNotEmpty()) upsertEvents(events)
        if (symbols.isNotEmpty()) insertSymbols(symbols)
    }

    @Query("DELETE FROM security_event WHERE kind = :kind AND publishedAt > 0 AND publishedAt < :cutoff")
    suspend fun deleteOlderThan(kind: String, cutoff: Long)
}
