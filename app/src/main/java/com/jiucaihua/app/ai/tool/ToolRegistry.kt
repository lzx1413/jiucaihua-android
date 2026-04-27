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
        val executor = executorsByName[name] ?: error("Tool not found: $name")
        return executor.execute(arguments)
    }
}
