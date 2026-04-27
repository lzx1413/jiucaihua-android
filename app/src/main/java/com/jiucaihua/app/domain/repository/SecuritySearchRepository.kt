package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.SecuritySearchResult

interface SecuritySearchRepository {
    suspend fun search(keyword: String, limit: Int = 20): List<SecuritySearchResult>
}
