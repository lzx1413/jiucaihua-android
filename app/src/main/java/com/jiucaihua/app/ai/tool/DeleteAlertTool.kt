package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.repository.AlertRepository
import javax.inject.Inject

class DeleteAlertTool @Inject constructor(
    private val alertRepository: AlertRepository,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "delete_alert",
        description = "删除指定 ID 的价格预警。可先通过 get_alerts 查询预警列表获取 ID。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "id" to mapOf(
                    "type" to "integer",
                    "description" to "预警 ID",
                ),
            ),
            "required" to listOf("id"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val id = when (val v = arguments["id"]) {
            is Number -> v.toLong()
            else -> return ToolResult(mapOf("success" to false, "error" to "缺少参数 id"))
        }

        val alert = alertRepository.getAlertById(id)
            ?: return ToolResult(mapOf("success" to false, "error" to "预警不存在: id=$id"))

        alertRepository.deleteAlert(id)
        return ToolResult(mapOf(
            "success" to true,
            "message" to "已删除预警：${alert.name}(${alert.code}) ${alert.alertType.label} ${alert.threshold}",
        ))
    }
}
