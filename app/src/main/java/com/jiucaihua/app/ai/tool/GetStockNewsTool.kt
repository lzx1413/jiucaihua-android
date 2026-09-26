package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.UnsupportedSecurityMarketException
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.SecurityEventRepository
import javax.inject.Inject

data class StockNewsSnapshot(
    val title: String,
    val summary: String,
    val source: String,
    val time: String?,
    val sourceType: String,
    val url: String = "",
    val kind: String? = null,
    val isStale: Boolean? = null,
)

data class StockNewsToolSnapshot(
    val keyword: String,
    val count: Int,
    val articles: List<StockNewsSnapshot>,
    val code: String? = null,
    val limit: Int,
    val possiblyTruncated: Boolean,
)

class GetStockNewsTool @Inject constructor(
    private val newsRepository: NewsRepository,
    private val securityEventRepository: SecurityEventRepository,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_stock_news",
        description = "获取指定个股/关键词的相关资讯。传入九财花规范code时按腾讯证券代码精确查询；仅传name时兼容本地6路快讯名称搜索。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "name" to mapOf(
                    "type" to "string",
                    "description" to "股票名称或关键词，例如\"贵州茅台\"、\"特斯拉\"、\"黄金行情\"等",
                ),
                "code" to mapOf(
                    "type" to "string",
                    "description" to "九财花规范证券代码，例如 sh600519、hk00700；提供时优先于name",
                ),
                "market_type" to mapOf(
                    "type" to "string",
                    "description" to "兼容字段，市场由code推导",
                ),
                "kinds" to mapOf(
                    "type" to "array",
                    "items" to mapOf("type" to "string", "enum" to GetStockEventsTool.SUPPORTED_KINDS.map { it.name }),
                ),
                "limit" to mapOf(
                    "type" to "integer",
                    "description" to "返回资讯条数，默认10",
                    "minimum" to 1, "maximum" to 50,
                ),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val name = arguments["name"] as? String
        val code = (arguments["code"] as? String)?.let {
            SecurityId.parse(it) ?: invalidArgs("code must be a supported normalized security code")
        }
        if (name.isNullOrBlank() && code == null) invalidArgs("one of name or code is required")
        val limit = (arguments["limit"] as? Number)?.toInt()?.coerceIn(1, 50) ?: 10
        if (code != null) {
            val kinds = GetStockEventsTool.parseKinds(arguments["kinds"])
            val events = try {
                securityEventRepository.getEvents(code, kinds, limit)
            } catch (error: UnsupportedSecurityMarketException) {
                unsupportedMarket(error.message ?: "unsupported market")
            }
            return ToolResult(
                StockNewsToolSnapshot(
                    keyword = name?.trim().orEmpty(),
                    code = code.value,
                    count = events.size,
                    limit = limit,
                    possiblyTruncated = events.size >= limit,
                    articles = events.map { event ->
                        StockNewsSnapshot(event.title, event.summary, event.publisher, event.publishedAt.takeIf { it > 0 }?.let { java.time.Instant.ofEpochMilli(it).toString() }, event.provider.name, event.contentUrl, event.kind.name, event.isStale)
                    },
                )
            )
        }
        val articles = newsRepository.searchNews(name!!.trim(), limit = limit)
        return ToolResult(StockNewsToolSnapshot(
            keyword = name.trim(),
            count = articles.size,
            limit = limit,
            possiblyTruncated = articles.size >= limit,
            articles = articles.map { it.toSnapshot() },
        ))
    }

    private fun NewsFlash.toSnapshot(): StockNewsSnapshot = StockNewsSnapshot(
        title = title,
        summary = summary,
        source = source,
        time = epochMillis.takeIf { it > 0 }?.let { java.time.Instant.ofEpochMilli(it).toString() },
        sourceType = sourceType.displayName,
        url = detailUrl,
    )
}
