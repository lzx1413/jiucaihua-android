package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.remote.datasource.TencentSecurityInsightDataSource
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.SecurityRelationsSnapshot
import com.jiucaihua.app.domain.repository.SecurityInsightRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityInsightRepositoryImpl @Inject constructor(
    private val dataSource: TencentSecurityInsightDataSource,
) : SecurityInsightRepository {
    override suspend fun getRelations(code: SecurityId): SecurityRelationsSnapshot = dataSource.getRelations(code)
}
