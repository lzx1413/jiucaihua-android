package com.jiucaihua.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "security_event_read",
    primaryKeys = ["provider", "kind", "externalId"],
)
data class SecurityEventReadEntity(
    val provider: String,
    val kind: String,
    val externalId: String,
    val readAt: Long,
)
