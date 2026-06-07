package com.jiucaihua.app.domain.model

data class StockArticle(
    val title: String,
    val summary: String,
    val content: String,
    val source: String,
    val time: String,
    val articleId: String? = null,
    val sourceType: NewsSource = NewsSource.JIUYAN,
    val impact: String = "",
)

fun StockArticle.toNewsFlash(): NewsFlash = NewsFlash(
    id = articleId?.hashCode()?.toLong() ?: title.hashCode().toLong(),
    title = title,
    summary = summary,
    content = content,
    impact = impact,
    source = source,
    time = time,
    sourceType = sourceType,
    epochMillis = 0L,
)
