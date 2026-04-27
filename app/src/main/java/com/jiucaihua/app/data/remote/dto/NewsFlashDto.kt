package com.jiucaihua.app.data.remote.dto

data class NewsFlashDto(
    val id: Long,
    val title: String,
    val summary: String,
    val content: String,
    val impact: String,
    val createdAt: Long,
)

data class StockArticleDto(
    val title: String,
    val content: String,
    val warnWords: String,
    val createTime: String,
    val articleId: String?,
)
