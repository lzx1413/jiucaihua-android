package com.jiucaihua.app.di

import com.jiucaihua.app.data.repository.AlertRepositoryImpl
import com.jiucaihua.app.data.repository.ExchangeRateRepositoryImpl
import com.jiucaihua.app.data.repository.FundRepositoryImpl
import com.jiucaihua.app.data.repository.HoldingRepositoryImpl
import com.jiucaihua.app.data.repository.MarketCalendarRepositoryImpl
import com.jiucaihua.app.data.repository.MarketRepositoryImpl
import com.jiucaihua.app.data.repository.NewsRepositoryImpl
import com.jiucaihua.app.data.repository.SecuritySearchRepositoryImpl
import com.jiucaihua.app.data.repository.StockRepositoryImpl
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.MarketRepository
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import com.jiucaihua.app.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHoldingRepository(
        impl: HoldingRepositoryImpl
    ): HoldingRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: StockRepositoryImpl
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindFundRepository(
        impl: FundRepositoryImpl
    ): FundRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl
    ): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindMarketCalendarRepository(
        impl: MarketCalendarRepositoryImpl
    ): MarketCalendarRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        impl: AlertRepositoryImpl
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): NewsRepository

    @Binds
    @Singleton
    abstract fun bindSecuritySearchRepository(
        impl: SecuritySearchRepositoryImpl
    ): SecuritySearchRepository

    @Binds
    @Singleton
    abstract fun bindMarketRepository(
        impl: MarketRepositoryImpl
    ): MarketRepository
}
