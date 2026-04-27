package com.jiucaihua.app.domain.model

data class NewsFlash(
    val id: Long,
    val title: String,
    val summary: String,
    val content: String,
    val impact: String,
    val source: String,
    val time: String,
)
