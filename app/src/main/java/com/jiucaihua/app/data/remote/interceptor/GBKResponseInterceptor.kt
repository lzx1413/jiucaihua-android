package com.jiucaihua.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.charset.Charset

class GBKResponseInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        val bytes = body.bytes()
        val decoded = String(bytes, Charset.forName("GB18030"))
        val newBody = decoded.toResponseBody(body.contentType())
        return response.newBuilder()
            .body(newBody)
            .build()
    }
}
