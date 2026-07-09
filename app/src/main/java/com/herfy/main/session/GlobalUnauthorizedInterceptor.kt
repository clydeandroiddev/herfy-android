package com.herfy.main.session

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean

object GlobalUnauthorizedInterceptor : Interceptor {

    private val isRegistered = AtomicBoolean(false)

    fun markRegisteredIfNeeded(): Boolean {
        return isRegistered.compareAndSet(false, true)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val hasAuthHeader = !request.header("Authorization").isNullOrEmpty()
        val isLogoutRequest = request.url.encodedPath.contains("logout", ignoreCase = true)

        if (response.code == 401 && hasAuthHeader && !isLogoutRequest) {
            SessionExpiryManager.notifySessionExpired()
        }

        return response
    }
}
