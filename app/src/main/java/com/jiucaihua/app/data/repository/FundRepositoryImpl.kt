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

        val quotes = coroutineScope {
            codes.map { code ->
                async { fetchSingleFund(code) }
            }.awaitAll().filterNotNull()
        }

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
        return FundQuote(
            code = fundcode,
            name = name,
            estimatedValue = gsz.toDoubleOrNull() ?: 0.0,
            dailyChangePercent = gszzl.toDoubleOrNull() ?: 0.0,
            netAssetValue = dwjz.toDoubleOrNull() ?: 0.0,
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
}
