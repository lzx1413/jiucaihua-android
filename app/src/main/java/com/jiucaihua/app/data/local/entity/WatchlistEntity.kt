package com.jiucaihua.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val marketType: String,
    @ColumnInfo(name = "group_name")
    val group: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
