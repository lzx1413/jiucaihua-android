package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildSearchResultsSnapshotUseCase
import javax.inject.Inject

class SearchSecuritiesTool @Inject constructor(
    private val buildSearchResultsSnapshotUseCase: BuildSearchResultsSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "search_securities",
        description = "按关键词搜索证券，返回匹配的代码、名称和市场类型，用于查找证券代码后调用其他工具。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "keyword" to mapOf(
                    "type" to "string",
                    "description" to "搜索关键词，如股票名称或代码",
                ),
                "limit" to mapOf(
                    "type" to "integer",
                    "description" to "返回的最大结果数，默认 20",
                ),
            ),
            "required" to listOf("keyword"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val keyword = arguments["keyword"] as? String ?: error("Missing required argument: keyword")
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 20
        return ToolResult(buildSearchResultsSnapshotUseCase(keyword.trim(), limit))
    }
}
