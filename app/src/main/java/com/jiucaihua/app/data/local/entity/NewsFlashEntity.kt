package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "news_flash",
    indices = [Index(value = ["newsId", "sourceType"], unique = true)],
)
data class NewsFlashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val newsId: Long,
    val title: String,
    val summary: String,
    val content: String,
    val impact: String,
    val source: String,
    val time: String,
    val sourceType: String,
    val epochMillis: Long,
    val detailUrl: String = "",
    val isBookmarked: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis(),
)
