package ua.kpi.iot_lighting.prac4.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val bearerHeaderProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (path.endsWith("/auth/login")) {
            return chain.proceed(original)
        }
        val authHeader = bearerHeaderProvider()
        val request = if (!authHeader.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", authHeader)
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
