package com.jiucaihua.app.ai.tool

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    executors: Set<@JvmSuppressWildcards ToolExecutor>,
) {
    private val executorsByName = executors.associateBy { it.name }

    fun getAll(): List<ToolExecutor> = executorsByName.values.sortedBy { it.name }

    fun getDefinitions(): List<ToolDefinition> = getAll().map { it.definition }

    fun get(name: String): ToolExecutor? = executorsByName[name]

    suspend fun execute(name: String, arguments: Map<String, Any?>): ToolResult {
        val executor = executorsByName[name] ?: return ToolResult(error = ToolError("NOT_FOUND", "Tool not found: $name"))
        return try {
            ToolArgumentValidator.validate(executor.inputSchema, arguments)
            executor.execute(arguments)
        } catch (error: ToolExecutionException) {
            ToolResult(error = error.toolError)
        } catch (error: java.io.IOException) {
            ToolResult(error = ToolError("PROVIDER_UNAVAILABLE", "Data provider request failed"))
        }
    }
}
