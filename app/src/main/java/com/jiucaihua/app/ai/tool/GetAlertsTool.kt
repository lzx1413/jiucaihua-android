package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.ai.usecase.BuildAlertsToolSnapshotUseCase
import javax.inject.Inject

class GetAlertsTool @Inject constructor(
    private val buildAlertsToolSnapshotUseCase: BuildAlertsToolSnapshotUseCase,
) : ToolExecutor {
    override val definition: ToolDefinition = ToolDefinition(
        name = "get_alerts",
        description = "获取全部或指定标的的预警快照，包含启用数量、近 24 小时触发次数和预警明细。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "可选。证券代码，不传则返回全部预警。",
                ),
            ),
            "required" to emptyList<String>(),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = (arguments["code"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        return ToolResult(buildAlertsToolSnapshotUseCase(code))
    }
}
