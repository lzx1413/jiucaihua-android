package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildWatchlistSnapshotUseCase
import javax.inject.Inject

class GetWatchlistTool @Inject constructor(
    private val buildWatchlistSnapshotUseCase: BuildWatchlistSnapshotUseCase,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "get_watchlist",
        description = "获取用户自选证券列表，返回每只证券的最新价格、涨跌幅和涨跌额。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        return ToolResult(buildWatchlistSnapshotUseCase())
    }
}
