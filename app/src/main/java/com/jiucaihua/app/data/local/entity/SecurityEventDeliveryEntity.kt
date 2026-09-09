package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "security_event_delivery",
    primaryKeys = ["provider", "kind", "externalId"],
    indices = [Index(value = ["symbol", "deliveredAt"])],
)
data class SecurityEventDeliveryEntity(
    val provider: String,
    val kind: String,
    val externalId: String,
    val symbol: String,
    val deliveredAt: Long,
)
