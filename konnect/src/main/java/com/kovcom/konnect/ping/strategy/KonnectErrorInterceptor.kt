package com.kovcom.konnect.ping.strategy

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * An OkHttp Interceptor that calls a provided action in case of network failures.
 * This allows the Konnect service to react to immediate network issues detected by OkHttp.
 */
class KonnectErrorInterceptor(private val onErrorAction: (Throwable) -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            return chain.proceed(request)
        } catch (e: IOException) {
            // Catch network-related exceptions and notify Konnect via the provided action
            onErrorAction(e)
            throw e // Re-throw the exception to propagate the error
        }
    }
}
