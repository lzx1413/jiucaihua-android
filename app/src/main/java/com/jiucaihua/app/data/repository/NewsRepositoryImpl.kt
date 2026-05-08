package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.remote.api.ClsNewsApi
import com.jiucaihua.app.data.remote.api.EastMoneyNewsApi
import com.jiucaihua.app.data.remote.api.Jin10Api
import com.jiucaihua.app.data.remote.api.JiuYanApi
import com.jiucaihua.app.data.remote.api.WallstreetCnApi
import com.jiucaihua.app.data.remote.api.XuanGuBaoNewsApi
import com.jiucaihua.app.data.remote.dto.StockArticleDto
import com.jiucaihua.app.data.remote.util.ClsSignHelper
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.NewsSource
import com.jiucaihua.app.domain.model.NewsTopic
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.domain.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val jiuYanApi: JiuYanApi,
    private val xuanGuBaoNewsApi: XuanGuBaoNewsApi,
    private val clsNewsApi: ClsNewsApi,
    private val wallstreetCnApi: WallstreetCnApi,
    private val jin10Api: Jin10Api,
    private val eastMoneyNewsApi: EastMoneyNewsApi,
) : NewsRepository {

    override suspend fun getMarketNews(limit: Int): List<NewsFlash> = withContext(Dispatchers.IO) {
        coroutineScope {
            val stcnDeferred = async { fetchStcnNews(limit) }
            val xgbDeferred = async { fetchXuanGuBaoNews(limit) }
            val clsDeferred = async { fetchClsNews(limit) }
            val wscnDeferred = async { fetchWallstreetCnNews(limit) }
            val jin10Deferred = async { fetchJin10News(limit) }
            val eastDeferred = async { fetchEastMoneyNews(limit) }

            val allNews = listOf(
                stcnDeferred.await(),
                xgbDeferred.await(),
                clsDeferred.await(),
                wscnDeferred.await(),
                jin10Deferred.await(),
                eastDeferred.await(),
            ).flatten()

            allNews
                .sortedByDescending { it.time }
                .take(limit)
        }
    }

    override suspend fun getMarketNews(topic: NewsTopic, limit: Int): List<NewsFlash> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferredResults = topic.sources.map { source ->
                async { fetchBySource(source, limit) }
            }
            deferredResults
                .map { it.await() }
                .flatten()
                .sortedByDescending { it.time }
                .take(limit)
        }
    }

    override suspend fun getStockNews(stockName: String, limit: Int): List<StockArticle> {
        val keyword = stockName.trim()
        if (keyword.isBlank()) return emptyList()

        return try {
            val token = getJiuYanToken()
            if (token.isBlank()) return emptyList()

            val payload = JSONObject()
                .put("back_garden", 0)
                .put("keyword", keyword)
                .put("order", 1)
                .put("limit", limit)
                .put("start", 0)
                .put("type", "1")
                .toString()

            val timestamp = System.currentTimeMillis().toString()
            val response = jiuYanApi.searchArticles(
                headers = mapOf(
                    "Origin" to "https://www.jiuyangongshe.com",
                    "platform" to "3",
                    "timestamp" to timestamp,
                    "token" to token,
                    "Referer" to "https://www.jiuyangongshe.com/",
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Accept" to "application/json, text/plain, */*",
                    "Content-Type" to "application/json",
                ),
                body = payload.toRequestBody(JSON_MEDIA_TYPE),
            )
            parseStockArticlesResponse(response)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- STCN (证券时报/人民财讯) ---

    private suspend fun fetchStcnNews(limit: Int): List<NewsFlash> {
        return try {
            val response = Jsoup.connect(STCN_LIST_URL)
                .ignoreContentType(true)
                .userAgent(DESKTOP_USER_AGENT)
                .referrer(STCN_LIST_REFERER)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .timeout(10_000)
                .execute()
                .body()
            parseStcnMarketNewsResponse(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStcnMarketNewsResponse(response: String, limit: Int): List<NewsFlash> {
        val root = JSONObject(response)
        if (root.optInt("state") != 1) return emptyList()

        val items = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(items.length(), limit)) {
                val item = items.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val summary = normalizeText(item.optString("content"))
                if (title.isBlank() || summary.isBlank()) continue
                val detailUrl = item.optString("url").toAbsoluteStcnUrl()
                val detail = fetchStcnDetail(detailUrl)
                add(
                    NewsFlash(
                        id = item.optLong("id"),
                        title = detail.title.ifBlank { title },
                        summary = abbreviate(summary, 140),
                        content = detail.content.ifBlank { summary },
                        impact = if (item.optInt("isRed") == 1) "利好" else "",
                        source = detail.source.ifBlank { item.optString("source").trim().ifBlank { STCN_SOURCE } },
                        time = formatEpochMillis(item.optLong("time")),
                        sourceType = NewsSource.STCN,
                    )
                )
            }
        }
    }

    private fun fetchStcnDetail(url: String): StcnDetail {
        if (url.isBlank()) return StcnDetail()
        return try {
            val document = Jsoup.connect(url)
                .userAgent(DESKTOP_USER_AGENT)
                .referrer(STCN_LIST_REFERER)
                .timeout(10_000)
                .get()
            StcnDetail(
                title = document.selectFirst(".detail-title")?.text().orEmpty().trim(),
                content = normalizeText(document.selectFirst(".detail-content")?.html().orEmpty()),
                source = document.selectFirst(".detail-info")
                    ?.text()
                    .orEmpty()
                    .substringAfter("来源：", "")
                    .substringBefore("作者：")
                    .trim(),
            )
        } catch (_: Exception) {
            StcnDetail()
        }
    }

    // --- XuanGuBao (选股宝) ---

    private suspend fun fetchXuanGuBaoNews(limit: Int): List<NewsFlash> {
        return try {
            val response = xuanGuBaoNewsApi.getNewsFlash(
                limit = limit,
                subjectIds = XGB_SUBJECT_IDS,
            )
            parseXuanGuBaoResponse(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseXuanGuBaoResponse(response: String, limit: Int): List<NewsFlash> {
        val root = JSONObject(response)
        if (root.optInt("code") != 20000) return emptyList()

        val messages = root.optJSONObject("data")?.optJSONArray("messages") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(messages.length(), limit)) {
                val item = messages.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val summary = normalizeText(item.optString("summary"))
                if (title.isBlank()) continue
                val impact = when (item.optInt("impact")) {
                    1 -> "利好"
                    -1 -> "利空"
                    else -> ""
                }
                add(
                    NewsFlash(
                        id = item.optLong("id"),
                        title = title,
                        summary = abbreviate(summary.ifBlank { title }, 140),
                        content = summary.ifBlank { title },
                        impact = impact,
                        source = XGB_SOURCE,
                        time = formatEpochSeconds(item.optLong("created_at")),
                        sourceType = NewsSource.XUANGUBAO,
                    )
                )
            }
        }
    }

    // --- CLS (财联社) ---

    private suspend fun fetchClsNews(limit: Int): List<NewsFlash> {
        return try {
            val params = mapOf(
                "app" to "CailianpressWeb",
                "os" to "web",
                "sv" to "7.7.5",
                "rn" to limit.toString(),
            )
            val sign = ClsSignHelper.calculateSign(params)
            val queryString = params.entries.joinToString("&") {
                "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
            }
            val url = "https://www.cls.cn/nodeapi/telegraphList?$queryString&sign=$sign"
            val response = clsNewsApi.getTelegraphList(
                url = url,
                headers = mapOf(
                    "User-Agent" to DESKTOP_USER_AGENT,
                    "Referer" to "https://www.cls.cn/telegraph",
                ),
            )
            parseClsResponse(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseClsResponse(response: String, limit: Int): List<NewsFlash> {
        val root = JSONObject(response)
        val rollData = root.optJSONObject("data")?.optJSONArray("roll_data") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(rollData.length(), limit)) {
                val item = rollData.optJSONObject(index) ?: continue
                if (item.optInt("is_ad") == 1) continue
                val title = item.optString("title").trim()
                val brief = normalizeText(item.optString("brief"))
                val content = normalizeText(item.optString("content"))
                if (title.isBlank() && brief.isBlank()) continue
                val displayTitle = title.ifBlank { brief }
                add(
                    NewsFlash(
                        id = item.optLong("id"),
                        title = displayTitle,
                        summary = abbreviate(brief.ifBlank { content }, 140),
                        content = content.ifBlank { brief },
                        impact = "",
                        source = CLS_SOURCE,
                        time = formatEpochSeconds(item.optLong("ctime")),
                        sourceType = NewsSource.CLS,
                    )
                )
            }
        }
    }

    // --- WallstreetCN (华尔街见闻) ---

    private suspend fun fetchWallstreetCnNews(limit: Int): List<NewsFlash> {
        return try {
            val response = wallstreetCnApi.getLives(
                channel = "global-channel",
                limit = limit,
            )
            parseWallstreetCnResponse(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseWallstreetCnResponse(response: String, limit: Int): List<NewsFlash> {
        val root = JSONObject(response)
        val items = root.optJSONObject("data")?.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(items.length(), limit)) {
                val item = items.optJSONObject(index) ?: continue
                val resourceType = item.optString("resource_type")
                if (resourceType == "theme" || resourceType == "ad") continue
                val contentText = normalizeText(item.optString("content_text"))
                val contentShort = normalizeText(item.optString("content_short"))
                if (contentText.isBlank()) continue
                val title = extractWscnTitle(contentText)
                add(
                    NewsFlash(
                        id = item.optLong("id"),
                        title = title,
                        summary = abbreviate(contentShort.ifBlank { contentText }, 140),
                        content = contentText,
                        impact = "",
                        source = WSCN_SOURCE,
                        time = formatEpochSeconds(item.optLong("display_time")),
                        sourceType = NewsSource.WALLSTREETCN,
                    )
                )
            }
        }
    }

    private fun extractWscnTitle(content: String): String {
        val bracketMatch = Regex("【(.+?)】").find(content)
        if (bracketMatch != null) return bracketMatch.groupValues[1]
        return content.take(30).let { if (it.length < content.length) it + "…" else it }
    }

    // --- Jin10 (金十数据) ---

    private suspend fun fetchJin10News(limit: Int): List<NewsFlash> {
        return try {
            val response = jin10Api.getFlashList(
                headers = mapOf(
                    "x-app-id" to JIN10_APP_ID,
                    "x-version" to JIN10_VERSION,
                ),
            )
            parseJin10Response(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJin10Response(response: String, limit: Int): List<NewsFlash> {
        val root = JSONObject(response)
        val data = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(data.length(), limit)) {
                val item = data.optJSONObject(index) ?: continue
                val type = item.optInt("type")
                val innerData = item.optJSONObject("data") ?: continue
                val id = item.optLong("id")
                val time = item.optString("time").trim()

                if (type == 0) {
                    val content = normalizeText(innerData.optString("content"))
                    if (content.isBlank()) continue
                    if (isAd(content)) continue
                    val title = extractJin10Title(content)
                    add(
                        NewsFlash(
                            id = id,
                            title = title,
                            summary = abbreviate(content, 140),
                            content = content,
                            impact = if (item.optBoolean("important")) "重要" else "",
                            source = JIN10_SOURCE,
                            time = formatJin10Time(time),
                            sourceType = NewsSource.JIN10,
                        )
                    )
                } else if (type == 1) {
                    val country = innerData.optString("country").trim()
                    val name = innerData.optString("name").trim()
                    val actual = innerData.optString("actual").trim()
                    val unit = innerData.optString("unit").trim()
                    val title = "$country$name"
                    val summary = "$title: $actual$unit"
                    if (title.isBlank()) continue
                    add(
                        NewsFlash(
                            id = id,
                            title = title,
                            summary = abbreviate(summary, 140),
                            content = summary,
                            impact = "",
                            source = JIN10_SOURCE,
                            time = formatJin10Time(time),
                            sourceType = NewsSource.JIN10,
                        )
                    )
                }
            }
        }
    }

    private fun isAd(content: String): Boolean {
        return Regex("""<a.*?>\s*<img.*?/></a>""", RegexOption.IGNORE_CASE).containsMatchIn(content)
    }

    private fun extractJin10Title(content: String): String {
        val bracketMatch = Regex("【(.+?)】").find(content)
        if (bracketMatch != null) return bracketMatch.groupValues[1]
        return content.take(30).let { if (it.length < content.length) it + "…" else it }
    }

    // --- East Money (东方财富) ---

    private suspend fun fetchEastMoneyNews(limit: Int): List<NewsFlash> {
        return try {
            val url = "https://newsapi.eastmoney.com/kuaixun?type=102&pagesize=$limit&pageindex=1"
            val response = eastMoneyNewsApi.getKuaixun(url)
            parseEastMoneyNewsResponse(response, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEastMoneyNewsResponse(response: String, limit: Int): List<NewsFlash> {
        val jsonStr = stripJsonp(response)
        val root = JSONObject(jsonStr)
        val data = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(data.length(), limit)) {
                val item = data.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val digest = normalizeText(item.optString("digest"))
                val content = normalizeText(item.optString("content"))
                if (title.isBlank()) continue
                add(
                    NewsFlash(
                        id = item.optString("id").hashCode().toLong(),
                        title = title,
                        summary = abbreviate(digest.ifBlank { content }, 140),
                        content = content.ifBlank { digest },
                        impact = "",
                        source = item.optString("source").trim().ifBlank { EASTMONEY_SOURCE },
                        time = item.optString("showtime").trim(),
                        sourceType = NewsSource.EASTMONEY,
                    )
                )
            }
        }
    }

    private fun stripJsonp(response: String): String {
        val trimmed = response.trim()
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        val jsonpMatch = Regex("""^[a-zA-Z_]\w*\((.*)\)$""", RegexOption.DOT_MATCHES_ALL).find(trimmed)
        if (jsonpMatch != null) return jsonpMatch.groupValues[1]
        return trimmed
    }

    private suspend fun fetchBySource(source: NewsSource, limit: Int): List<NewsFlash> {
        return when (source) {
            NewsSource.STCN -> fetchStcnNews(limit)
            NewsSource.XUANGUBAO -> fetchXuanGuBaoNews(limit)
            NewsSource.CLS -> fetchClsNews(limit)
            NewsSource.WALLSTREETCN -> fetchWallstreetCnNews(limit)
            NewsSource.JIN10 -> fetchJin10News(limit)
            NewsSource.EASTMONEY -> fetchEastMoneyNews(limit)
            NewsSource.JIUYAN -> emptyList()
        }
    }

    // --- JiuYan (韭研公社) stock news ---

    private suspend fun getJiuYanToken(): String {
        val response = jiuYanApi.getTokenSource(
            url = BAIDU_HM_URL,
            headers = mapOf(
                "Cookie" to "HMACCOUNT=50E5CAF378DF1999; HMACCOUNT_BFESS=50E5CAF378DF1999",
                "Referer" to "https://www.jiuyangongshe.com/",
                "User-Agent" to DESKTOP_USER_AGENT,
                "Accept" to "*/*",
                "Accept-Language" to "zh-CN,zh;q=0.9",
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache",
            ),
        )
        return response.headers()["etag"]?.trim('"').orEmpty()
    }

    private fun parseStockArticlesResponse(response: String): List<StockArticle> {
        val root = JSONObject(response)
        if (root.optString("errCode").let { it.isNotBlank() && it != "0" }) return emptyList()

        val data = root.optJSONObject("data") ?: return emptyList()
        val result = data.optJSONArray("result") ?: data.optJSONArray("data") ?: return emptyList()

        return buildList {
            for (index in 0 until result.length()) {
                val item = result.optJSONObject(index) ?: continue
                val dto = StockArticleDto(
                    title = item.optString("title").trim(),
                    content = item.optString("content"),
                    warnWords = item.optString("warn_words").trim(),
                    createTime = item.optString("create_time").trim(),
                    articleId = item.optString("article_id").ifBlank { null },
                )
                if (dto.title.isBlank()) continue
                add(dto.toDomain())
            }
        }
    }

    private fun StockArticleDto.toDomain(): StockArticle {
        val summary = buildSummary(content, warnWords)
        val normalizedContent = normalizeText(content)
        return StockArticle(
            title = title,
            summary = summary,
            content = normalizedContent.ifBlank { summary },
            source = "韭研公社",
            time = createTime,
            articleId = articleId,
            sourceType = NewsSource.JIUYAN,
        )
    }

    // --- Common utilities ---

    private fun buildSummary(content: String, warnWords: String): String {
        val cleanContent = normalizeText(content)
        val cleanWarnWords = normalizeText(warnWords)
        val merged = listOf(cleanWarnWords, cleanContent)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        return merged.takeIf { it.isNotBlank() }?.let { abbreviate(it, 140) } ?: "暂无摘要"
    }

    private fun normalizeText(text: String): String {
        return Jsoup.parse(text).text().replace(WHITESPACE_REGEX, " ").trim()
    }

    private fun abbreviate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength - 1) + "…"
    }

    private fun formatEpochMillis(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(TIME_FORMATTER)
    }

    private fun formatEpochSeconds(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(TIME_FORMATTER)
    }

    private fun formatJin10Time(time: String): String {
        if (time.isBlank()) return ""
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(time, formatter)
            localDateTime.format(TIME_FORMATTER)
        } catch (_: Exception) {
            time
        }
    }

    private data class StcnDetail(
        val title: String = "",
        val content: String = "",
        val source: String = "",
    )

    private fun String.toAbsoluteStcnUrl(): String {
        if (isBlank()) return ""
        return if (startsWith("http://") || startsWith("https://")) this else STCN_BASE_URL + this
    }

    companion object {
        private const val BAIDU_HM_URL = "https://hm.baidu.com/hm.js?58aa18061df7855800f2a1b32d6da7f4"
        private const val STCN_BASE_URL = "https://www.stcn.com"
        private const val STCN_LIST_URL = "https://www.stcn.com/article/list.html?type=kx"
        private const val STCN_LIST_REFERER = "https://www.stcn.com/article/list/kx.html"
        private const val STCN_SOURCE = "人民财讯"
        private const val XGB_SUBJECT_IDS = "9,10,723,35,469"
        private const val XGB_SOURCE = "选股宝"
        private const val CLS_SOURCE = "财联社"
        private const val WSCN_SOURCE = "华尔街见闻"
        private const val JIN10_SOURCE = "金十数据"
        private const val EASTMONEY_SOURCE = "东方财富"
        private const val JIN10_APP_ID = "bVBF4FyRTn5NJF5n"
        private const val JIN10_VERSION = "1.0.0"
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
