package com.jiucaihua.app.data.remote.datasource

import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.model.UnsupportedSecurityMarketException

/** Endpoint-specific Tencent symbol conversion. Unsupported markets never fall back silently. */
class SecurityCodeMapper {
    fun toQuoteSymbol(code: SecurityId): Result<String> = when {
        code.value.startsWith("hk") -> Result.success("r_${code.value}")
        code.value.startsWith("sh") || code.value.startsWith("sz") || code.value.startsWith("bj") -> Result.success(code.value)
        code.value.startsWith("usr_") -> Result.success("us${code.value.removePrefix("usr_").uppercase()}")
        else -> Result.failure(UnsupportedSecurityMarketException(code.value))
    }

    fun toKLineSymbol(code: SecurityId): Result<String> = when {
        code.value.startsWith("usr_") -> Result.success("us.${code.value.removePrefix("usr_").uppercase()}")
        code.value.startsWith("sh") || code.value.startsWith("sz") || code.value.startsWith("bj") || code.value.startsWith("hk") -> Result.success(code.value)
        else -> Result.failure(UnsupportedSecurityMarketException(code.value))
    }

    fun toNewsSymbol(code: SecurityId): Result<String> = when {
        code.value.startsWith("sh") || code.value.startsWith("sz") || code.value.startsWith("bj") || code.value.startsWith("hk") || code.value.startsWith("usr_") -> Result.success(code.value)
        else -> Result.failure(UnsupportedSecurityMarketException(code.value))
    }
}
