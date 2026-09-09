package com.jiucaihua.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurityIdTest {
    @Test
    fun `normalizes supported codes and derives market`() {
        assertEquals("sh600519", SecurityId.parse(" SH600519 ")?.value)
        assertEquals(MarketType.HK_STOCK, SecurityId.parse("hk00700")?.marketType)
        assertEquals("usr_aapl", SecurityId.parse("usr_AAPL")?.value)
    }

    @Test
    fun `rejects provider-specific and malformed codes`() {
        assertNull(SecurityId.parse("usAAPL"))
        assertNull(SecurityId.parse("sh60051"))
        assertNull(SecurityId.parse("not-a-security"))
    }
}
