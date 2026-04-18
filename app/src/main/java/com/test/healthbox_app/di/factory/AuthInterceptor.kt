package com.test.healthbox_app.di.factory

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val token: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", token) // ⚡ pass token
            .addHeader("accept", "application/json")
            .build()
        return chain.proceed(newRequest)
    }
}
