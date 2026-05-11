package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import javax.inject.Inject

class CreateAlertTool @Inject constructor(
    private val alertRepository: AlertRepository,
) : ToolExecutor {
    override val definition = ToolDefinition(
        name = "create_alert",
        description = "为指定证券创建价格预警。预警类型包括：PRICE_ABOVE（价格高于）、PRICE_BELOW（价格低于）、CHANGE_ABOVE（涨幅超过）、CHANGE_BELOW（跌幅超过）。价格类阈值为具体价格数值，涨跌幅类阈值为百分比数值（如 5 表示 5%）。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "证券代码，如 600519、00700",
                ),
                "name" to mapOf(
                    "type" to "string",
                    "description" to "证券名称，如 贵州茅台",
                ),
                "alertType" to mapOf(
                    "type" to "string",
                    "description" to "预警类型：PRICE_ABOVE、PRICE_BELOW、CHANGE_ABOVE、CHANGE_BELOW",
                    "enum" to AlertType.entries.map { it.name },
                ),
                "threshold" to mapOf(
                    "type" to "number",
                    "description" to "阈值。价格类为具体价格，涨跌幅类为百分比（如 5 表示 5%）",
                ),
            ),
            "required" to listOf("code", "name", "alertType", "threshold"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = (arguments["code"] as? String)?.takeIf { it.isNotBlank() }
            ?: return ToolResult(mapOf("success" to false, "error" to "缺少参数 code"))

        val name = (arguments["name"] as? String)?.takeIf { it.isNotBlank() }
            ?: return ToolResult(mapOf("success" to false, "error" to "缺少参数 name"))

        val alertTypeStr = (arguments["alertType"] as? String)?.takeIf { it.isNotBlank() }
            ?: return ToolResult(mapOf("success" to false, "error" to "缺少参数 alertType"))

        val alertType = try {
            AlertType.valueOf(alertTypeStr)
        } catch (_: IllegalArgumentException) {
            return ToolResult(mapOf(
                "success" to false,
                "error" to "无效的 alertType: $alertTypeStr，可选值: ${AlertType.entries.joinToString()}",
            ))
        }

        val threshold = when (val t = arguments["threshold"]) {
            is Number -> t.toDouble()
            else -> return ToolResult(mapOf("success" to false, "error" to "缺少参数 threshold"))
        }

        val alert = PriceAlert(
            code = code,
            name = name,
            alertType = alertType,
            threshold = threshold,
        )
        val id = alertRepository.addAlert(alert)

        return ToolResult(mapOf(
            "success" to true,
            "id" to id,
            "message" to "已创建预警：$name($code) ${alertType.label} $threshold",
        ))
    }
}
