package com.kovcom.konnect.okhttp.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class KonnectErrorInterceptor(
    private val onErrorAction: (Throwable) -> Unit,

    ) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            val proceeded = chain.proceed(request)
            if (!proceeded.isSuccessful) {
                Log.e("TAG", "Error code: ${proceeded.code} for ${request.url}")
//                onErrorAction(IOException("HTTP error code: ${proceeded.code}"))
            }
            return proceeded
        } catch (e: IOException) {
            Log.e("TAG", "IOException for ${chain.request().url}: $e")
            onErrorAction(e)
            throw e
        }
    }
}