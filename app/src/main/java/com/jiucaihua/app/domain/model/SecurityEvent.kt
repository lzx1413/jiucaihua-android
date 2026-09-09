package com.jiucaihua.app.domain.model

enum class DataProvider { TENCENT, SINA, EASTMONEY }

enum class DataServedFrom { NETWORK, MEMORY_CACHE, ROOM_CACHE }

enum class SecurityEventKind {
    NEWS,
    ANNOUNCEMENT,
    PERIODIC_REPORT,
    RESEARCH,
    INDUSTRY_NEWS,
}

data class SecurityEvent(
    val externalId: String,
    val provider: DataProvider = DataProvider.TENCENT,
    val kind: SecurityEventKind,
    val title: String,
    val summary: String = "",
    val contentUrl: String = "",
    val publisher: String = "",
    val publishedAt: Long = 0L,
    val symbols: List<SecurityId>,
    val importance: Int? = null,
    val titleMention: Int? = null,
    val bodyMention: Int? = null,
    val researchRating: String? = null,
    val reportType: String? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val servedFrom: DataServedFrom = DataServedFrom.NETWORK,
    val isStale: Boolean = false,
    val warnings: List<String> = emptyList(),
    val isRead: Boolean = false,
)

data class StockFundFlowSnapshot(
    val code: SecurityId,
    val currency: String = "CNY",
    val unit: String = "CNY",
    val sourceUpdatedAt: Long? = null,
    val mainNet: Double? = null,
    val retailNet: Double? = null,
    val superLargeNet: Double? = null,
    val largeNet: Double? = null,
    val mediumNet: Double? = null,
    val smallNet: Double? = null,
    val mainNetPercent: Double? = null,
    val retailNetPercent: Double? = null,
    val intradayPoints: List<StockFundFlowPoint> = emptyList(),
    val fiveDaySummary: List<StockFundFlowPoint> = emptyList(),
    val provider: DataProvider = DataProvider.TENCENT,
    val fetchedAt: Long = System.currentTimeMillis(),
    val servedFrom: DataServedFrom = DataServedFrom.NETWORK,
    val isStale: Boolean = false,
    val warnings: List<String> = emptyList(),
)

data class StockFundFlowPoint(
    val time: String,
    val mainNet: Double? = null,
    val retailNet: Double? = null,
)
