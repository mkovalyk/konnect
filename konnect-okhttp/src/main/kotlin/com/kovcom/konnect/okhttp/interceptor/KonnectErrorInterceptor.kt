package com.kovcom.konnect.okhttp.interceptor 
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException 
class KonnectErrorInterceptor(
    private val onErrorAction: (Throwable) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            return chain.proceed(request)
        } catch (e: IOException) {
            onErrorAction(e)
            throw e
        }
    }
}