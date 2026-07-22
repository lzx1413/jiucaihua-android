package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.local.dao.FundCacheDao
import com.jiucaihua.app.data.local.entity.FundCacheEntity
import com.jiucaihua.app.data.remote.api.FundApi
import com.jiucaihua.app.data.remote.dto.FundQuoteDto
import com.jiucaihua.app.domain.model.FundQuote
import com.jiucaihua.app.domain.model.KLineData
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.KLinePoint
import com.jiucaihua.app.domain.repository.FundRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FundRepositoryImpl @Inject constructor(
    private val fundApi: FundApi,
    private val fundCacheDao: FundCacheDao,
) : FundRepository {

    override suspend fun getFundQuotes(codes: List<String>): List<FundQuote> {
        if (codes.isEmpty()) return emptyList()

        val requestedCodes = codes.distinct()
        val remoteQuotes = coroutineScope {
            requestedCodes.map { code ->
                async { fetchSingleFund(code) }
            }.awaitAll().filterNotNull()
        }
        val usableRemoteQuotes = remoteQuotes.filter { it.hasUsableValue() }
        val remoteCodes = usableRemoteQuotes.mapTo(mutableSetOf()) { it.code }
        val cachedQuotes = getCachedFundQuotes(requestedCodes.filterNot { it in remoteCodes })
        val usableCachedQuotes = cachedQuotes.filter { it.hasUsableValue() }
        val availableCodes = (remoteCodes + usableCachedQuotes.map { it.code }).toSet()
        // QDII funds often have no intraday estimate. In that case, retrieve the
        // latest confirmed NAV instead of treating the price as unavailable.
        val confirmedNavQuotes = coroutineScope {
            requestedCodes.filterNot { it in availableCodes }
                .map { code -> async { fetchLatestConfirmedNav(code) } }
                .awaitAll()
                .filterNotNull()
        }
        val quotes = usableRemoteQuotes + usableCachedQuotes + confirmedNavQuotes

        if (quotes.isNotEmpty()) {
            val cacheEntities = quotes.map { it.toCacheEntity() }
            fundCacheDao.insertAll(cacheEntities)
        }

        return quotes
    }

    override suspend fun getCachedFundQuotes(codes: List<String>): List<FundQuote> {
        if (codes.isEmpty()) return emptyList()
        return fundCacheDao.getByCodes(codes).map { it.toDomain() }
    }

    override suspend fun getFundNavHistory(code: String, limit: Int): KLineData {
        val url = "https://fundf10.eastmoney.com/F10DataApi.aspx?type=lsjz&code=$code&page=1&per=$limit"
        val response = fundApi.getFundEstimate(url)
        return parseFundNavHtml(code, response)
    }

    private fun parseFundNavHtml(code: String, html: String): KLineData {
        val points = mutableListOf<KLinePoint>()

        val rowPattern = Pattern.compile("<tr><td>(\\d{4}-\\d{2}-\\d{2})</td><td class='tor bold'>(\\d+\\.\\d+)</td>")
        val matcher = rowPattern.matcher(html)

        while (matcher.find()) {
            val date = matcher.group(1) ?: continue
            val nav = matcher.group(2)?.toDoubleOrNull() ?: continue
            points.add(
                KLinePoint(
                    date = date,
                    open = nav,
                    close = nav,
                    high = nav,
                    low = nav,
                    volume = 0.0,
                )
            )
        }

        points.reverse()

        return KLineData(code = code, name = "", period = KLinePeriod.DAILY, points = points)
    }

    private suspend fun fetchSingleFund(code: String): FundQuote? {
        return try {
            val url = "https://fundgz.1234567.com.cn/js/$code.js"
            val response = fundApi.getFundEstimate(url)
            val dto = parseJsonpResponse(response) ?: return null
            dto.toDomain()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchLatestConfirmedNav(code: String): FundQuote? {
        return try {
            // fundgz does not publish intraday estimates for many QDII funds.
            // Its old F10DataApi fallback has also been retired, while the F10
            // NAV page still renders the latest confirmed unit NAV server-side.
            val response = fundApi.getFundEstimate("https://fundf10.eastmoney.com/jjjz_$code.html")
            val latestNav = parseLatestConfirmedNavFromPage(response) ?: return null
            latestNav.value.takeIf { it > 0 }?.let { nav ->
                FundQuote(
                    code = code,
                    name = code,
                    estimatedValue = nav,
                    dailyChangePercent = 0.0,
                    netAssetValue = nav,
                    estimateTime = "",
                    navDate = latestNav.date,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonpResponse(response: String): FundQuoteDto? {
        val regex = Regex("jsonpgz\\((.*)\\);?")
        val match = regex.find(response) ?: return null
        val jsonStr = match.groupValues[1]
        if (jsonStr.isBlank()) return null

        val json = JSONObject(jsonStr)
        val code = json.optString("fundcode", "")
        if (code.isBlank()) return null

        return FundQuoteDto(
            fundcode = code,
            name = json.optString("name", ""),
            gsz = json.optString("gsz", "--"),
            gszzl = json.optString("gszzl", "--"),
            dwjz = json.optString("dwjz", "--"),
            jzrq = json.optString("jzrq", ""),
            gztime = json.optString("gztime", ""),
        )
    }

    private fun FundQuoteDto.toDomain(): FundQuote {
        val estimatedValue = gsz.toDoubleOrNull()
        val netAssetValue = dwjz.toDoubleOrNull()
        return FundQuote(
            code = fundcode,
            name = name,
            // During a source outage gsz is often "--" while the last confirmed NAV
            // is still present. A NAV is preferable to treating the fund as worthless.
            estimatedValue = estimatedValue?.takeIf { it > 0 } ?: netAssetValue ?: 0.0,
            dailyChangePercent = gszzl.toDoubleOrNull() ?: 0.0,
            netAssetValue = netAssetValue ?: 0.0,
            estimateTime = gztime,
            navDate = jzrq,
        )
    }

    private fun FundQuote.toCacheEntity(): FundCacheEntity {
        return FundCacheEntity(
            code = code,
            name = name,
            estimatedValue = estimatedValue,
            dailyChangePercent = dailyChangePercent,
            netAssetValue = netAssetValue,
            estimateTime = estimateTime,
            navDate = navDate,
        )
    }

    private fun FundCacheEntity.toDomain(): FundQuote {
        return FundQuote(
            code = code,
            name = name,
            estimatedValue = estimatedValue,
            dailyChangePercent = dailyChangePercent,
            netAssetValue = netAssetValue,
            estimateTime = estimateTime,
            navDate = navDate,
        )
    }

    private fun FundQuote.hasUsableValue(): Boolean {
        return estimatedValue > 0 || netAssetValue > 0
    }

}

internal data class ConfirmedFundNav(
    val date: String,
    val value: Double,
)

internal fun parseLatestConfirmedNavFromPage(response: String): ConfirmedFundNav? {
    val match = LATEST_NAV_PATTERN.find(response) ?: return null
    val date = match.groupValues[1]
    val value = match.groupValues[2].toDoubleOrNull() ?: return null
    return ConfirmedFundNav(date = date, value = value)
}

private val LATEST_NAV_PATTERN = Regex(
    """单位净值（(\d{2}-\d{2})）：\s*<b[^>]*>\s*([0-9.]+)""",
    RegexOption.DOT_MATCHES_ALL,
)
