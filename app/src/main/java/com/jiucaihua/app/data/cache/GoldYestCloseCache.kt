package com.jiucaihua.app.data.cache

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 黄金昨收价缓存。
 *
 * 上海金交所黄金品种（如 gds_AUTD、gds_AU9999）有日盘和夜盘两个交易时段。
 * 新浪API在日盘返回的 yestClose 是前一夜盘收盘价，在夜盘返回的 yestClose 是当日日盘收盘价。
 * 这导致"今日收益"的计算基准在两个时段之间不一致。
 *
 * 解决方案：日盘首次获取行情时缓存 yestClose，夜盘使用缓存的 yestClose 作为参考基准，
 * 确保同一交易日内收益计算使用一致的昨收价。
 */
@Singleton
class GoldYestCloseCache @Inject constructor(
    @Named("appPrefs") private val prefs: SharedPreferences,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 获取今日缓存的昨收价，如果没有缓存返回 null */
    fun getYestClose(code: String): Double? {
        val today = dateFormat.format(Date())
        val cachedDate = prefs.getString(KEY_DATE, null)
        if (cachedDate != today) return null // 日期变更，缓存失效

        val key = yestCloseKey(code)
        val value = prefs.getString(key, null)
        return value?.toDoubleOrNull()
    }

    /** 缓存昨收价，同时记录日期以确保跨日自动失效 */
    fun cacheYestClose(code: String, yestClose: Double) {
        val today = dateFormat.format(Date())
        val editor = prefs.edit()

        // 检查日期是否变更，如果是则清除所有旧的昨收价缓存
        val cachedDate = prefs.getString(KEY_DATE, null)
        if (cachedDate != today) {
            // 清除所有 gold_yestClose_* 的缓存
            val keysToRemove = prefs.all.keys.filter { it.startsWith(KEY_YESTCLOSE_PREFIX) }
            for (key in keysToRemove) {
                editor.remove(key)
            }
            editor.putString(KEY_DATE, today)
        }

        editor.putString(yestCloseKey(code), yestClose.toString())
        editor.apply()
    }

    private fun yestCloseKey(code: String): String = "$KEY_YESTCLOSE_PREFIX$code"

    companion object {
        private const val KEY_DATE = "gold_yestClose_date"
        private const val KEY_YESTCLOSE_PREFIX = "gold_yestClose_"
    }
}