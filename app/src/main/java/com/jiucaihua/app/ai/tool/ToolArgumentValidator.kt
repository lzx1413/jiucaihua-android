package com.jiucaihua.app.ai.tool

/** Validate the schema we advertise before any repository calls or mutations. */
internal object ToolArgumentValidator {
    fun validate(schema: Map<String, Any?>, arguments: Map<String, Any?>) {
        validateValue("arguments", arguments, schema)
        val from = (arguments["from"] as? Number)?.toLong()
        val to = (arguments["to"] as? Number)?.toLong()
        if (from != null && to != null && from > to) invalidArgs("from must not exceed to")
    }

    private fun validateValue(path: String, value: Any?, schema: Map<*, *>) {
        val valid = when (schema["type"]) {
            "object" -> value is Map<*, *>
            "array" -> value is List<*>
            "string" -> value is String && value.isNotBlank()
            "boolean" -> value is Boolean
            "number" -> value is Number && value.toDouble().isFinite()
            "integer" -> value is Number && value.toDouble().isFinite() && value.toDouble() % 1.0 == 0.0
            else -> true
        }
        if (!valid) invalidArgs("$path must be ${schema["type"]}")
        (schema["enum"] as? List<*>)?.let {
            if (value !in it) invalidArgs("$path must be one of ${it.joinToString()}")
        }
        if (value is Number) {
            val number = value.toDouble()
            (schema["minimum"] as? Number)?.let { if (number < it.toDouble()) invalidArgs("$path must be >= $it") }
            (schema["maximum"] as? Number)?.let { if (number > it.toDouble()) invalidArgs("$path must be <= $it") }
            (schema["exclusiveMinimum"] as? Number)?.let { if (number <= it.toDouble()) invalidArgs("$path must be > $it") }
        }
        if (value is List<*>) {
            (schema["items"] as? Map<*, *>)?.let { itemSchema ->
                value.forEachIndexed { index, item -> validateValue("$path[$index]", item, itemSchema) }
            }
        }
        if (value is Map<*, *>) {
            val properties = schema["properties"] as? Map<*, *> ?: emptyMap<Any, Any>()
            (schema["required"] as? List<*>)?.forEach { if (value[it] == null) invalidArgs("$it is required") }
            value.forEach { (key, item) ->
                val property = properties[key] as? Map<*, *> ?: invalidArgs("unknown argument: $key")
                validateValue("$path.$key", item, property)
            }
        }
    }
}
