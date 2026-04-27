package com.jiucaihua.app.di

import com.jiucaihua.app.data.remote.api.FundApi
import com.jiucaihua.app.data.remote.api.HolidayApi
import com.jiucaihua.app.data.remote.api.JiuYanApi
import com.jiucaihua.app.data.remote.api.SinaStockApi
import com.jiucaihua.app.data.remote.api.TencentHKStockApi
import com.jiucaihua.app.data.remote.api.TencentKLineApi
import com.jiucaihua.app.data.remote.api.TencentSearchApi
import com.jiucaihua.app.data.remote.api.XuanGuBaoNewsApi
import com.jiucaihua.app.data.remote.interceptor.GBKResponseInterceptor
import com.jiucaihua.app.data.remote.interceptor.RandomUserAgentInterceptor
import com.jiucaihua.app.data.remote.interceptor.SinaRefererInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("default")
    fun provideDefaultOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RandomUserAgentInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("sina")
    fun provideSinaOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(SinaRefererInterceptor())
            .addInterceptor(RandomUserAgentInterceptor())
            .addInterceptor(GBKResponseInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("tencent")
    fun provideTencentOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RandomUserAgentInterceptor())
            .addInterceptor(GBKResponseInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("sina")
    fun provideSinaRetrofit(@Named("sina") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hq.sinajs.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("tencent")
    fun provideTencentRetrofit(@Named("tencent") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://qt.gtimg.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("tencentSearch")
    fun provideTencentSearchRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://proxy.finance.qq.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("fund")
    fun provideFundRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://fundgz.1234567.com.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSinaStockApi(@Named("sina") retrofit: Retrofit): SinaStockApi {
        return retrofit.create(SinaStockApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTencentHKStockApi(@Named("tencent") retrofit: Retrofit): TencentHKStockApi {
        return retrofit.create(TencentHKStockApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTencentSearchApi(@Named("tencentSearch") retrofit: Retrofit): TencentSearchApi {
        return retrofit.create(TencentSearchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFundApi(@Named("fund") retrofit: Retrofit): FundApi {
        return retrofit.create(FundApi::class.java)
    }

    @Provides
    @Singleton
    @Named("tencentKLine")
    fun provideTencentKLineRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://web.ifzq.gtimg.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTencentKLineApi(@Named("tencentKLine") retrofit: Retrofit): TencentKLineApi {
        return retrofit.create(TencentKLineApi::class.java)
    }

    @Provides
    @Singleton
    @Named("holiday")
    fun provideHolidayRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://timor.tech/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("xuanGuBao")
    fun provideXuanGuBaoRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://baoer-api.xuangubao.com.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("jiuYan")
    fun provideJiuYanRetrofit(@Named("default") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://app.jiuyangongshe.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHolidayApi(@Named("holiday") retrofit: Retrofit): HolidayApi {
        return retrofit.create(HolidayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideXuanGuBaoNewsApi(@Named("xuanGuBao") retrofit: Retrofit): XuanGuBaoNewsApi {
        return retrofit.create(XuanGuBaoNewsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJiuYanApi(@Named("jiuYan") retrofit: Retrofit): JiuYanApi {
        return retrofit.create(JiuYanApi::class.java)
    }
}
