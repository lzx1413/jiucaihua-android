package com.jiucaihua.app.domain.model

data class SecurityRelationItem(
    val code: String,
    val name: String,
)

data class SecurityRelationsSnapshot(
    val code: SecurityId,
    val plates: List<SecurityRelationItem> = emptyList(),
    val relatedSecurities: List<SecurityRelationItem> = emptyList(),
    val shareholderReportPeriod: String? = null,
    val shareholders: List<SecurityShareholder> = emptyList(),
    val provider: DataProvider = DataProvider.TENCENT,
    val fetchedAt: Long = System.currentTimeMillis(),
    val servedFrom: DataServedFrom = DataServedFrom.NETWORK,
    val isStale: Boolean = false,
    val warnings: List<String> = emptyList(),
)

data class SecurityShareholder(
    val name: String,
    val holdingShares: Double?,
    val floatPercent: Double?,
    val previousHoldingShares: Double?,
    val shareNature: String,
)
