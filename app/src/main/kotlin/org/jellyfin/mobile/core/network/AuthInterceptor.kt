package org.jellyfin.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that attaches standard Jellyfin authorization headers:
 * - X-Emby-Authorization: MediaBrowser Client="mdd TV", Device="Android", DeviceId="mdd-tv-android", Version="1.0.0"
 * - X-Emby-Token: <token> (appended when an active session token exists)
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    companion object {
        const val CLIENT_NAME = "mdd TV"
        const val DEVICE_NAME = "Android"
        const val DEVICE_ID = "mdd-tv-android"
        const val CLIENT_VERSION = "1.0.0"

        val AUTH_HEADER_VALUE =
            """MediaBrowser Client="$CLIENT_NAME", Device="$DEVICE_NAME", DeviceId="$DEVICE_ID", Version="$CLIENT_VERSION""""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("X-Emby-Authorization", AUTH_HEADER_VALUE)

        val token = sessionManager.getToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("X-Emby-Token", token)
        }

        return chain.proceed(requestBuilder.build())
    }
}
