package com.jiucaihua.app.data.remote.datasource

import com.jiucaihua.app.data.remote.api.TencentSecurityInsightApi
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.SecurityRelationItem
import com.jiucaihua.app.domain.model.SecurityRelationsSnapshot
import com.jiucaihua.app.domain.model.SecurityShareholder
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TencentSecurityInsightDataSource @Inject constructor(
    private val api: TencentSecurityInsightApi,
    private val codeMapper: SecurityCodeMapper,
) {
    suspend fun getRelations(code: SecurityId): SecurityRelationsSnapshot = supervisorScope {
        val tencentCode = codeMapper.toNewsSymbol(code).getOrThrow()
        val plates = async { safely(emptyList()) { TencentRelationParser.parse(api.getPlates(tencentCode)) } }
        val related = async { safely(emptyList()) { TencentRelationParser.parse(api.getRelatedSecurities(tencentCode)) } }
        val shareholders = async { safely(TencentShareholderResponse(null, emptyList())) { TencentShareholderParser.parse(api.getShareholders(tencentCode)) } }
        val parsedShareholders = shareholders.await()
        SecurityRelationsSnapshot(
            code = code,
            plates = plates.await(),
            relatedSecurities = related.await(),
            shareholderReportPeriod = parsedShareholders.reportPeriod,
            shareholders = parsedShareholders.items,
        )
    }

    private suspend fun <T> safely(default: T, block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        default
    }
}

internal data class TencentShareholderResponse(val reportPeriod: String?, val items: List<SecurityShareholder>)

internal object TencentShareholderParser {
    fun parse(response: String): TencentShareholderResponse {
        val root = JSONObject(response)
        if (root.optInt("code", -1) != 0) return TencentShareholderResponse(null, emptyList())
        val latest = root.optJSONObject("data")?.optJSONArray("data")?.optJSONObject(0)
            ?: return TencentShareholderResponse(null, emptyList())
        val items = latest.optJSONArray("rows") ?: JSONArray()
        return TencentShareholderResponse(
            reportPeriod = latest.optString("pdt").trim().ifBlank { null },
            items = buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val name = item.optString("gdmc").trim()
                    if (name.isBlank()) continue
                    add(
                        SecurityShareholder(
                            name = name,
                            holdingShares = item.optString("cgsl").toDoubleOrNull(),
                            floatPercent = item.optString("ltbl").toDoubleOrNull(),
                            previousHoldingShares = item.optString("sqcgsl").toDoubleOrNull(),
                            shareNature = item.optString("gfxz").trim(),
                        )
                    )
                }
            },
        )
    }
}

internal object TencentRelationParser {
    fun parse(response: String): List<SecurityRelationItem> {
        val root = JSONObject(response)
        if (root.optInt("code", -1) != 0) return emptyList()
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val rawCode = item.optString("code").trim()
                if (name.isNotBlank() && rawCode.isNotBlank()) add(SecurityRelationItem(rawCode, name))
            }
        }
    }
}
