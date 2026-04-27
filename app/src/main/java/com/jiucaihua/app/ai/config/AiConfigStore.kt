package com.jiucaihua.app.ai.config

import com.jiucaihua.app.ai.model.AiConfig

interface AiConfigStore {
    fun get(): AiConfig
    fun save(config: AiConfig)
}
