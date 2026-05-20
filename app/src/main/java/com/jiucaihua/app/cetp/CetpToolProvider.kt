package com.jiucaihua.app.cetp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.jiucaihua.app.ai.tool.ToolRegistry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

class CetpToolProvider : ContentProvider() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Volatile
    private var toolRegistry: ToolRegistry? = null

    private fun getRegistry(): ToolRegistry {
        return toolRegistry ?: EntryPointAccessors
            .fromApplication<CetpToolEntryPoint>(context!!.applicationContext)
            .toolRegistry()
            .also { toolRegistry = it }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            "list_tools" -> handleListTools()
            "execute_tool" -> handleExecuteTool(extras)
            "get_provider_info" -> handleGetProviderInfo()
            else -> errorBundle("TOOL_NOT_FOUND", "Unknown method: $method")
        }
    }

    private fun handleListTools(): Bundle {
        val registry = getRegistry()
        val toolsArray = JSONArray()
        for (name in EXTERNAL_TOOLS) {
            val def = registry.get(name)?.definition ?: continue
            val toolJson = JSONObject().apply {
                put("name", "$NAMESPACE_PREFIX$name")
                put("description", def.description)
                put("parameters", JSONObject(def.inputSchema as Map<String, Any?>))
            }
            toolsArray.put(toolJson)
        }
        val data = JSONObject().apply {
            put("tools", toolsArray)
        }
        return successBundle(data.toString())
    }

    private fun handleExecuteTool(extras: Bundle?): Bundle {
        val toolName = extras?.getString("tool_name")
            ?: return errorBundle("INVALID_ARGS", "missing tool_name")

        val localName = toolName.removePrefix(NAMESPACE_PREFIX)
        if (!toolName.startsWith(NAMESPACE_PREFIX) || !EXTERNAL_TOOLS.contains(localName)) {
            return errorBundle("TOOL_NOT_FOUND", "Unknown tool: $toolName")
        }

        val argsJson = extras.getString("args") ?: "{}"
        val args = try {
            val obj = JSONObject(argsJson)
            obj.keys().asSequence().associateWith { obj.get(it) }
        } catch (e: Exception) {
            return errorBundle("INVALID_ARGS", "Failed to parse args: ${e.message}")
        }

        val registry = getRegistry()
        val executor = registry.get(localName)
            ?: return errorBundle("TOOL_NOT_FOUND", "Unknown tool: $toolName")

        return try {
            val result = runBlocking(Dispatchers.IO) {
                registry.execute(localName, args)
            }
            val content = result.content
            val json = if (content != null) {
                when (content) {
                    is Map<*, *> -> {
                        val obj = JSONObject()
                        for ((key, value) in content) {
                            obj.put(key.toString(), value ?: JSONObject.NULL)
                        }
                        obj.toString(2)
                    }
                    else -> {
                        val adapter = moshi.adapter<Any>(content::class.java)
                        adapter.indent("  ").toJson(content) ?: "null"
                    }
                }
            } else {
                "null"
            }
            successBundle(json)
        } catch (e: Exception) {
            errorBundle("INTERNAL_ERROR", e.message ?: "Unknown error")
        }
    }

    private fun handleGetProviderInfo(): Bundle {
        val data = JSONObject().apply {
            put("provider_name", "Jiucaihua")
            put("description", "个人投资管理数据，包含持仓、行情、资讯、预警和市场资金流向")
            put("scopes", JSONArray().apply {
                put(JSONObject().put("name", "portfolio").put("description", "投资组合与持仓"))
                put(JSONObject().put("name", "market").put("description", "市场行情与资金流向"))
                put(JSONObject().put("name", "news").put("description", "市场资讯"))
                put(JSONObject().put("name", "alerts").put("description", "价格预警"))
                put(JSONObject().put("name", "search").put("description", "证券搜索"))
                put(JSONObject().put("name", "watchlist").put("description", "自选证券列表"))
            })
        }
        return successBundle(data.toString())
    }

    private fun successBundle(data: String): Bundle {
        return Bundle().apply {
            putString("status", "success")
            putString("data", data)
        }
    }

    private fun errorBundle(code: String, message: String): Bundle {
        return Bundle().apply {
            putString("status", "error")
            putString("error_code", code)
            putString("error_message", message)
        }
    }

    override fun onCreate() = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?,
    ): Int = 0

    companion object {
        const val NAMESPACE_PREFIX = "jiucaihua__"

        val EXTERNAL_TOOLS = setOf(
            "get_portfolio_analysis",
            "get_holding_analysis",
            "get_kline_data",
            "get_indicator_snapshot",
            "get_market_news",
            "get_alerts",
            "create_alert",
            "delete_alert",
            "calculate_what_if",
            "get_market_indices",
            "get_fund_flow",
            "search_securities",
            "get_market_status",
            "get_watchlist",
        )
    }
}
