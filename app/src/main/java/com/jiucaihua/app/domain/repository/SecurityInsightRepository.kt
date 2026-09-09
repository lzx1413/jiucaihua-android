package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.SecurityRelationsSnapshot

interface SecurityInsightRepository {
    suspend fun getRelations(code: SecurityId): SecurityRelationsSnapshot
}
