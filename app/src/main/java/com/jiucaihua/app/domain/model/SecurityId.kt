package com.jiucaihua.app.domain.model

/** A normalized security identifier used outside data-source specific adapters. */
@JvmInline
value class SecurityId(val value: String) {
    val marketType: MarketType
        get() = MarketType.fromCode(value)

    companion object {
        fun parse(raw: String): SecurityId? {
            val code = raw.trim().lowercase()
            val valid = when {
                code.matches(Regex("(sh|sz|bj)[0-9]{6}")) -> true
                code.matches(Regex("hk[0-9]{5}")) -> true
                code.matches(Regex("(usr|gb)_[a-z0-9.]+")) -> true
                code.matches(Regex("[0-9]{6}")) -> true
                else -> false
            }
            return code.takeIf { valid }?.let(::SecurityId)
        }
    }
}

class UnsupportedSecurityMarketException(code: String) : IllegalArgumentException("UNSUPPORTED_MARKET: $code")
