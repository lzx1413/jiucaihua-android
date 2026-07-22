package com.jiucaihua.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class FundRepositoryImplTest {

    @Test
    fun `parses the latest confirmed NAV from QDII fund page`() {
        val nav = parseLatestConfirmedNavFromPage(
            """
            <label>
                单位净值（07-21）：
            <b class="grn lar bold">
                1.5425 ( -0.34% )</b>
            """.trimIndent(),
        )

        assertEquals("07-21", nav?.date)
        assertEquals(1.5425, nav?.value ?: 0.0, 0.0001)
    }
}
