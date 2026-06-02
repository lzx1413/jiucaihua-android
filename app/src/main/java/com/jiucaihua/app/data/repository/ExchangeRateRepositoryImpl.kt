package com.jiucaihua.app.data.repository

import android.content.SharedPreferences
import com.jiucaihua.app.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    @Named("default") private val okHttpClient: OkHttpClient,
    @Named("appPrefs") private val prefs: SharedPreferences,
) : ExchangeRateRepository {

    override suspend fun getHkdToCnyRate(): Double {
        val cached = getCachedRate()
        if (cached != null && !isCacheExpired()) {
            return cached
        }

        return try {
            val rate = fetchRateFromBoc()
            if (rate > 0) {
                saveRateToCache(rate)
                rate
            } else {
                cached ?: DEFAULT_HKD_RATE
            }
        } catch (_: Exception) {
            cached ?: DEFAULT_HKD_RATE
        }
    }

    private suspend fun fetchRateFromBoc(): Double = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BOC_URL)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext 0.0

        val doc = Jsoup.parse(html)
        val rows = doc.select("table").getOrNull(1)?.select("tr") ?: return@withContext 0.0

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 5) continue
            val currencyName = cells[0].text().trim()
            if (currencyName.contains("港币")) {
                val spotBuyPrice = cells[1].text().trim().toDoubleOrNull() ?: continue
                return@withContext spotBuyPrice / 100.0
            }
        }

        0.0
    }

    private fun getCachedRate(): Double? {
        val rate = prefs.getFloat(KEY_HKD_RATE, -1f)
        return if (rate > 0) rate.toDouble() else null
    }

    private fun isCacheExpired(): Boolean {
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        return System.currentTimeMillis() - lastUpdate > CACHE_DURATION_MS
    }

    private fun saveRateToCache(rate: Double) {
        prefs.edit()
            .putFloat(KEY_HKD_RATE, rate.toFloat())
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    override suspend fun getUsdToCnyRate(): Double {
        val cached = getCachedUsdRate()
        if (cached != null && !isUsdCacheExpired()) {
            return cached
        }

        return try {
            val rate = fetchUsdRateFromBoc()
            if (rate > 0) {
                saveUsdRateToCache(rate)
                rate
            } else {
                cached ?: DEFAULT_USD_RATE
            }
        } catch (_: Exception) {
            cached ?: DEFAULT_USD_RATE
        }
    }

    private suspend fun fetchUsdRateFromBoc(): Double = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BOC_URL)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext 0.0

        val doc = Jsoup.parse(html)
        val rows = doc.select("table").getOrNull(1)?.select("tr") ?: return@withContext 0.0

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 5) continue
            val currencyName = cells[0].text().trim()
            if (currencyName.contains("美元")) {
                val spotBuyPrice = cells[1].text().trim().toDoubleOrNull() ?: continue
                return@withContext spotBuyPrice / 100.0
            }
        }

        0.0
    }

    private fun getCachedUsdRate(): Double? {
        val rate = prefs.getFloat(KEY_USD_RATE, -1f)
        return if (rate > 0) rate.toDouble() else null
    }

    private fun isUsdCacheExpired(): Boolean {
        val lastUpdate = prefs.getLong(KEY_USD_LAST_UPDATE, 0)
        return System.currentTimeMillis() - lastUpdate > CACHE_DURATION_MS
    }

    private fun saveUsdRateToCache(rate: Double) {
        prefs.edit()
            .putFloat(KEY_USD_RATE, rate.toFloat())
            .putLong(KEY_USD_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val BOC_URL = "https://www.boc.cn/sourcedb/whpj/index.html"
        private const val KEY_HKD_RATE = "hkd_to_cny_rate"
        private const val KEY_LAST_UPDATE = "hkd_rate_last_update"
        private const val KEY_USD_RATE = "usd_to_cny_rate"
        private const val KEY_USD_LAST_UPDATE = "usd_rate_last_update"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
        const val DEFAULT_HKD_RATE = 0.92
        const val DEFAULT_USD_RATE = 7.2
    }
}
