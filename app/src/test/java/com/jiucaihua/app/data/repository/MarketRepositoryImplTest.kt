package com.jiucaihua.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketRepositoryImplTest {

    @Test
    fun normalizeSouthboundNetInflow_usesNetBuyWhenNetInflowIsQuotaValue() {
        val result = normalizeSouthboundNetInflow(
            dayNetAmtIn = 4_200_000.0,
            dayAmtRemain = 0.0,
            dayAmtThreshold = 4_200_000.0,
            netBuyAmt = 446_813.26,
        )

        assertEquals(446_813.26, result, 0.01)
    }

    @Test
    fun normalizeSouthboundNetInflow_keepsNetInflowWhenItIsNotQuotaValue() {
        val result = normalizeSouthboundNetInflow(
            dayNetAmtIn = 120_000.0,
            dayAmtRemain = 4_080_000.0,
            dayAmtThreshold = 4_200_000.0,
            netBuyAmt = 80_000.0,
        )

        assertEquals(120_000.0, result, 0.01)
    }
}
