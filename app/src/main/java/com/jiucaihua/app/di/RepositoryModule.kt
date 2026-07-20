package com.jiucaihua.app.di

import com.jiucaihua.app.data.repository.AlertRepositoryImpl
import com.jiucaihua.app.data.repository.ExchangeRateRepositoryImpl
import com.jiucaihua.app.data.repository.FundRepositoryImpl
import com.jiucaihua.app.data.repository.HoldingRepositoryImpl
import com.jiucaihua.app.data.repository.HoldingSnapshotRepositoryImpl
import com.jiucaihua.app.data.repository.MarketCalendarRepositoryImpl
import com.jiucaihua.app.data.repository.MarketRepositoryImpl
import com.jiucaihua.app.data.repository.NewsRepositoryImpl
import com.jiucaihua.app.data.repository.PortfolioSnapshotRepositoryImpl
import com.jiucaihua.app.data.repository.SecuritySearchRepositoryImpl
import com.jiucaihua.app.data.repository.StockRepositoryImpl
import com.jiucaihua.app.data.repository.TransactionRepositoryImpl
import com.jiucaihua.app.data.repository.TransactionLotMatchRepositoryImpl
import com.jiucaihua.app.data.repository.WatchlistRepositoryImpl
import com.jiucaihua.app.domain.repository.AlertRepository
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import com.jiucaihua.app.domain.repository.FundRepository
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.HoldingSnapshotRepository
import com.jiucaihua.app.domain.repository.MarketCalendarRepository
import com.jiucaihua.app.domain.repository.MarketRepository
import com.jiucaihua.app.domain.repository.NewsRepository
import com.jiucaihua.app.domain.repository.PortfolioSnapshotRepository
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import com.jiucaihua.app.domain.repository.StockRepository
import com.jiucaihua.app.domain.repository.TransactionRepository
import com.jiucaihua.app.domain.repository.TransactionLotMatchRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
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
    abstract fun bindHoldingSnapshotRepository(
        impl: HoldingSnapshotRepositoryImpl,
    ): HoldingSnapshotRepository

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

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(
        impl: WatchlistRepositoryImpl
    ): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioSnapshotRepository(
        impl: PortfolioSnapshotRepositoryImpl
    ): PortfolioSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTransactionLotMatchRepository(
        impl: TransactionLotMatchRepositoryImpl
    ): TransactionLotMatchRepository
}
