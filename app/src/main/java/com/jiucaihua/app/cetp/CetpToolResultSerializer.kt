package com.jiucaihua.app.cetp

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal class CetpToolResultSerializer {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    fun toJson(content: Any?): String {
        if (content == null) return "null"
        // Missing values are omitted; zero remains a valid numeric value.
        // Compact output is important because CETP results are copied into the
        // agent conversation history by ClawSeed.
        return moshi.adapter(Any::class.java).toJson(content)
    }
}
