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
