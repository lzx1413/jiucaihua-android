package com.jiucaihua.app.data.repository

import com.jiucaihua.app.data.remote.api.JiuYanApi
import com.jiucaihua.app.data.remote.dto.StockArticleDto
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.StockArticle
import com.jiucaihua.app.domain.repository.NewsRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val jiuYanApi: JiuYanApi,
) : NewsRepository {

    override suspend fun getMarketNews(limit: Int): List<NewsFlash> = withContext(Dispatchers.IO) {
        return@withContext try {
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
        )
    }

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
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
