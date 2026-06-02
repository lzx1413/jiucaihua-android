package com.jiucaihua.app.domain.repository

interface ExchangeRateRepository {
    suspend fun getHkdToCnyRate(): Double
    suspend fun getUsdToCnyRate(): Double
}
