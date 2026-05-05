package com.jiucaihua.app.cetp

import com.jiucaihua.app.ai.tool.ToolRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CetpToolEntryPoint {
    fun toolRegistry(): ToolRegistry
}
