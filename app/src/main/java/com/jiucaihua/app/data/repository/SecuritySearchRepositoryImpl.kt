package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.remote.api.TencentSearchApi
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SecuritySearchResult
import com.jiucaihua.app.domain.repository.SecuritySearchRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuritySearchRepositoryImpl @Inject constructor(
    private val tencentSearchApi: TencentSearchApi,
) : SecuritySearchRepository {

    override suspend fun search(keyword: String, limit: Int): List<SecuritySearchResult> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        return parseSearchResponse(
            response = tencentSearchApi.search(query),
            limit = limit,
        )
    }

    private fun parseSearchResponse(response: String, limit: Int): List<SecuritySearchResult> {
        val root = JSONObject(response)
        if (root.optInt("code", -1) != 0) return emptyList()

        val data = root.optJSONObject("data") ?: return emptyList()
        val items = sequenceOf("stock", "fund", "relatedFund")
            .mapNotNull { data.optJSONArray(it) }
            .flatMap { array -> (0 until array.length()).asSequence().mapNotNull(array::optJSONArray) }
            .toList()
        val seen = mutableSetOf<String>()

        return buildList {
            for (item in items) {
                if (size >= limit) break
                val sourceMarket = item.optString(0).trim().lowercase()
                val rawCode = item.optString(1).trim()
                val name = item.optString(2).trim()
                val marketType = sourceMarket.toMarketType() ?: continue
                val normalizedCode = normalizeCode(sourceMarket, rawCode) ?: continue
                if (name.isBlank()) continue

                val dedupeKey = "$sourceMarket:$rawCode"
                if (!seen.add(dedupeKey)) continue

                add(
                    SecuritySearchResult(
                        code = normalizedCode,
                        displayCode = rawCode.uppercase(),
                        name = name,
                        marketType = marketType,
                    )
                )
            }
        }
    }

    private fun normalizeCode(sourceMarket: String, rawCode: String): String? {
        if (rawCode.isBlank()) return null

        return when (sourceMarket) {
            "sh", "sz", "bj" -> rawCode.takeIf { it.all(Char::isDigit) }?.let { "$sourceMarket$it" }
            "hk" -> rawCode.takeIf { it.all(Char::isDigit) }?.padStart(5, '0')?.let { "hk$it" }
            "jj" -> rawCode.takeIf { it.all(Char::isDigit) }
            "us" -> rawCode.takeIf { it.matches(Regex("[A-Za-z0-9.]+")) }?.let { "usr_${it.uppercase()}" }
            else -> null
        }
    }

    private fun String.toMarketType(): MarketType? {
        return when (this) {
            "sh", "sz", "bj" -> MarketType.A_STOCK
            "hk" -> MarketType.HK_STOCK
            "jj" -> MarketType.FUND
            "us" -> MarketType.US_STOCK
            else -> null
        }
    }
}
