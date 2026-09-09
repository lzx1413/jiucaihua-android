package com.jiucaihua.app.data.remote.datasource

import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class TencentRemoteDataSourceParserTest {
    @Test
    fun `parses event fields and preserves associated symbols`() {
        val events = TencentEventParser.parse(
            """{"code":0,"data":{"data":[{"id":"news-1","title":"公告标题","summary":"摘要","url":"https://example.com/a","src":"原始媒体","time":"2026-09-09 10:30:00","symbols":["sh600519","hk00700"],"importance":3}]}}""",
            SecurityEventKind.NEWS,
            SecurityId("sh600519"),
        )

        assertEquals(1, events.size)
        assertEquals("news-1", events.single().externalId)
        assertEquals(listOf("sh600519", "hk00700"), events.single().symbols.map { it.value })
        assertEquals("https://example.com/a", events.single().contentUrl)
        assertEquals(3, events.single().importance)
    }

    @Test
    fun `parses stock fund flow with numeric fields and trend`() {
        val flow = TencentFundFlowParser.parse(
            """{"code":0,"data":{"todayFundFlow":{"mainNetIn":"100","retailIn":"80","retailOut":"20","superFlow":"60","bigFlow":"40","normalFlow":"-10","smallFlow":"-5","mainInRate":"30","mainOutRate":"10","retailInRate":"20","retailOutRate":"25"},"todayFundTrend":{"minList":[{"time":"202609091000","MainNetInflow":"100","RetailNetInflow":"-100"}]},"fiveDayFundFlow":[]}}""",
            SecurityId("sh600519"),
        )

        assertNotNull(flow)
        assertEquals(100.0, flow?.mainNet)
        assertEquals(60.0, flow?.retailNet)
        assertEquals(1, flow?.intradayPoints?.size)
        assertFalse(flow?.warnings.orEmpty().isEmpty())
    }

    @Test
    fun `parses relation securities without assuming plate codes are security ids`() {
        val relations = TencentRelationParser.parse(
            """{"code":0,"data":[{"code":"012010","name":"酿酒"},{"code":"sh601318","name":"中国平安"}]}""",
        )

        assertEquals(2, relations.size)
        assertEquals("012010", relations[0].code)
        assertEquals("中国平安", relations[1].name)
    }

    @Test
    fun `uses latest shareholder report and keeps numeric values nullable`() {
        val result = TencentShareholderParser.parse(
            """{"code":0,"data":{"data":[{"pdt":"2026-06-30","rows":[{"gdmc":"股东甲","cgsl":"1000","ltbl":"1.25","sqcgsl":"900","gfxz":"国有股"},{"gdmc":"股东乙","cgsl":"","ltbl":"","gfxz":""}]}]}}""",
        )

        assertEquals("2026-06-30", result.reportPeriod)
        assertEquals(2, result.items.size)
        assertEquals(1000.0, result.items[0].holdingShares)
        assertEquals(null, result.items[1].holdingShares)
    }
}
