package com.jiucaihua.app.ai.tool

import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.PriceAlert
import com.jiucaihua.app.domain.repository.AlertRepository
import javax.inject.Inject

class CreateAlertTool @Inject constructor(
    private val alertRepository: AlertRepository,
) : ToolExecutor {
    private val SUPPORTED_TYPES = setOf(AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW, AlertType.CHANGE_ABOVE, AlertType.CHANGE_BELOW)

    override val definition = ToolDefinition(
        name = "create_alert",
        description = "为指定证券创建价格预警。预警类型包括：PRICE_ABOVE（价格高于）、PRICE_BELOW（价格低于）、CHANGE_ABOVE（涨幅超过）、CHANGE_BELOW（跌幅超过）。价格类阈值为具体价格数值，涨跌幅类阈值为百分比数值（如 5 表示 5%）。可选提供操作提示，如加仓500股、减仓、止盈等。",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "code" to mapOf(
                    "type" to "string",
                    "description" to "证券代码，如 sh600519、hk00700",
                ),
                "name" to mapOf(
                    "type" to "string",
                    "description" to "证券名称，如 贵州茅台",
                ),
                "alertType" to mapOf(
                    "type" to "string",
                    "description" to "预警类型：PRICE_ABOVE、PRICE_BELOW、CHANGE_ABOVE、CHANGE_BELOW",
                    "enum" to SUPPORTED_TYPES.map { it.name },
                ),
                "threshold" to mapOf(
                    "type" to "number",
                    "exclusiveMinimum" to 0,
                    "description" to "阈值。价格类为具体价格，涨跌幅类为百分比（如 5 表示 5%）",
                ),
                "actionHint" to mapOf(
                    "type" to "string",
                    "description" to "操作提示，如加仓500股、减仓、止盈、止损、观望等。触发预警时会显示此提示。",
                ),
            ),
            "required" to listOf("code", "name", "alertType", "threshold"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val code = (arguments["code"] as? String)?.takeIf { it.isNotBlank() }
            ?: invalidArgs("code is required")

        val name = (arguments["name"] as? String)?.takeIf { it.isNotBlank() }
            ?: invalidArgs("name is required")

        val alertTypeStr = (arguments["alertType"] as? String)?.takeIf { it.isNotBlank() }
            ?: invalidArgs("alertType is required")

        val alertType = try {
            AlertType.valueOf(alertTypeStr)
        } catch (_: IllegalArgumentException) {
            invalidArgs("alertType must be one of: ${AlertType.entries.joinToString()}")
        }

        val threshold = when (val t = arguments["threshold"]) {
            is Number -> t.toDouble()
            else -> invalidArgs("threshold is required")
        }

        if (alertType !in SUPPORTED_TYPES) invalidArgs("alertType requires unsupported parameters")
        if (!threshold.isFinite() || threshold <= 0) invalidArgs("threshold must be positive")
        if (com.jiucaihua.app.domain.model.SecurityId.parse(code) == null && !code.matches(Regex("(?:hf_|gds_)[A-Za-z0-9]+"))) invalidArgs("code must be normalized, e.g. sh600519")

        val actionHint = (arguments["actionHint"] as? String)?.takeIf { it.isNotBlank() }

        val alert = PriceAlert(
            code = code,
            name = name,
            alertType = alertType,
            threshold = threshold,
            actionHint = actionHint,
        )
        val id = alertRepository.addAlert(alert)

        val hintMsg = if (actionHint != null) "，操作提示：$actionHint" else ""
        return ToolResult(mapOf(
            "success" to true,
            "id" to id,
            "message" to "已创建预警：$name($code) ${alertType.label} $threshold$hintMsg",
        ))
    }
}
