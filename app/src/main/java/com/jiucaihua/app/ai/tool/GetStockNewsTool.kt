package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.repository.NewsRepository
import javax.inject.Inject

data class StockNewsSnapshot(
    val title: String,
    val summary: String,
    val source: String,
    val time: String,
    val sourceType: String,
)

data class StockNewsToolSnapshot(
    val keyword: String,
    val count: Int,
    val articles: List<StockNewsSnapshot>,
)

class GetStockNewsTool @Inject constructor(
    private val newsRepository: NewsRepository,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_stock_news",
        description = "获取指定个股/关键词的相关资讯，返回标题、摘要、来源和时间。通过name参数传入股票名称或关键词查询相关新闻。数据来自本地缓存的6大资讯源（财联社、选股宝、华尔街见闻、金十、东方财富、人民财讯）。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "name" to mapOf(
                    "type" to "string",
                    "description" to "股票名称或关键词，例如\"贵州茅台\"、\"特斯拉\"、\"黄金行情\"等",
                ),
                "limit" to mapOf(
                    "type" to "integer",
                    "description" to "返回资讯条数，默认10",
                ),
            ),
            "required" to listOf("name"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val name = arguments["name"] as? String ?: error("Missing required argument: name")
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 10
        val articles = newsRepository.searchNews(name.trim(), limit = limit)
        return ToolResult(StockNewsToolSnapshot(
            keyword = name.trim(),
            count = articles.size,
            articles = articles.map { it.toSnapshot() },
        ))
    }

    private fun NewsFlash.toSnapshot(): StockNewsSnapshot = StockNewsSnapshot(
        title = title,
        summary = summary,
        source = source,
        time = time,
        sourceType = sourceType.displayName,
    )
}