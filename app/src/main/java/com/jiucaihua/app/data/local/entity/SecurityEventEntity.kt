package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "security_event",
    primaryKeys = ["provider", "kind", "externalId"],
    indices = [Index(value = ["publishedAt"]), Index(value = ["contentUrl"])],
)
data class SecurityEventEntity(
    val provider: String,
    val kind: String,
    val externalId: String,
    val title: String,
    val summary: String,
    val contentUrl: String,
    val publisher: String,
    val publishedAt: Long,
    val importance: Int?,
    val titleMention: Int?,
    val bodyMention: Int?,
    val researchRating: String?,
    val reportType: String?,
    val fetchedAt: Long,
)

@Entity(
    tableName = "security_event_symbol",
    primaryKeys = ["provider", "kind", "externalId", "symbol"],
    indices = [Index(value = ["symbol", "provider", "kind"]), Index(value = ["externalId"])],
)
data class SecurityEventSymbolEntity(
    val provider: String,
    val kind: String,
    val externalId: String,
    val symbol: String,
)

@Entity(
    tableName = "security_event_sync_state",
    primaryKeys = ["provider", "kind", "symbol"],
)
data class SecurityEventSyncStateEntity(
    val provider: String,
    val kind: String,
    val symbol: String,
    val lastSuccessfulSyncAt: Long,
    val newestPublishedAt: Long,
    val nextPage: Int = 1,
)
