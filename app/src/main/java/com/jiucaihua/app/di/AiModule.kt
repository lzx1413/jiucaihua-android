package com.jiucaihua.app.di

import com.jiucaihua.app.ai.config.AiConfigStore
import com.jiucaihua.app.ai.config.EncryptedAiConfigStore
import com.jiucaihua.app.ai.orchestrator.LlmAgentClient
import com.jiucaihua.app.ai.orchestrator.OpenAiCompatibleLlmAgentClient
import com.jiucaihua.app.ai.provider.AiProviderService
import com.jiucaihua.app.ai.provider.DefaultAiProviderService
import com.jiucaihua.app.ai.tool.CalculateWhatIfTool
import com.jiucaihua.app.ai.tool.GetAlertsTool
import com.jiucaihua.app.ai.tool.GetHoldingAnalysisTool
import com.jiucaihua.app.ai.tool.GetKLineDataTool
import com.jiucaihua.app.ai.tool.GetMarketNewsTool
import com.jiucaihua.app.ai.tool.GetPortfolioAnalysisTool
import com.jiucaihua.app.ai.tool.ToolExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindAiConfigStore(
        store: EncryptedAiConfigStore,
    ): AiConfigStore

    @Binds
    abstract fun bindAiProviderService(
        service: DefaultAiProviderService,
    ): AiProviderService

    @Binds
    abstract fun bindLlmAgentClient(
        client: OpenAiCompatibleLlmAgentClient,
    ): LlmAgentClient

    @Binds
    @IntoSet
    abstract fun bindGetPortfolioAnalysisTool(
        tool: GetPortfolioAnalysisTool,
    ): ToolExecutor

    @Binds
    @IntoSet
    abstract fun bindGetHoldingAnalysisTool(
        tool: GetHoldingAnalysisTool,
    ): ToolExecutor

    @Binds
    @IntoSet
    abstract fun bindGetKLineDataTool(
        tool: GetKLineDataTool,
    ): ToolExecutor

    @Binds
    @IntoSet
    abstract fun bindGetMarketNewsTool(
        tool: GetMarketNewsTool,
    ): ToolExecutor

    @Binds
    @IntoSet
    abstract fun bindGetAlertsTool(
        tool: GetAlertsTool,
    ): ToolExecutor

    @Binds
    @IntoSet
    abstract fun bindCalculateWhatIfTool(
        tool: CalculateWhatIfTool,
    ): ToolExecutor
}
