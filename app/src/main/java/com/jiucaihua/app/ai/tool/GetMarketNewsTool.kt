package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildMarketNewsDigestUseCase
import com.jiucaihua.app.domain.model.NewsTopic
import javax.inject.Inject

class GetMarketNewsTool @Inject constructor(
    private val buildMarketNewsDigestUseCase: BuildMarketNewsDigestUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_market_news",
        description = "获取市场资讯摘要，返回最新资讯列表和生成时间。可通过topic参数筛选特定领域的新闻，避免返回过多无关数据。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "topic" to mapOf(
                    "type" to "string",
                    "description" to "新闻主题筛选：A_STOCK=A股，GLOBAL=国际宏观，FUTURES=期货商品，US_STOCK=美股，FOREX=外汇。不传则返回所有主题的混合资讯。",
                    "enum" to NewsTopic.entries.map { it.name },
                ),
                "query" to mapOf(
                    "type" to "string",
                    "description" to "搜索关键词，在标题和内容中匹配。例如\"降息\"、\"特斯拉\"、\"AI\"等，用于精准查找相关资讯。",
                ),
                "limit" to mapOf(
                    "type" to "integer",
                    "description" to "返回的资讯条数，默认 10",
                ),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 10
        val topicName = arguments["topic"] as? String
        val topic = topicName?.let { name ->
            NewsTopic.entries.find { it.name == name }
        }
        val query = arguments["query"] as? String
        return ToolResult(buildMarketNewsDigestUseCase(limit, topic, query))
    }
}
