package com.jiucaihua.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class SinaRefererInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Referer", "https://finance.sina.com.cn/")
            .build()
        return chain.proceed(request)
    }
}
