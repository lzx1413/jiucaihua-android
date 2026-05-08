package com.jiucaihua.app.data.remote.util

import java.net.URLEncoder
import java.security.MessageDigest

object ClsSignHelper {

    fun calculateSign(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val paramStr = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val encoded = URLEncoder.encode(paramStr, "UTF-8")
        val sha1 = sha1Hex(encoded)
        return md5Hex(sha1)
    }

    private fun sha1Hex(input: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun md5Hex(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
