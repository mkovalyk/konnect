package com.kovcom.konnect.network

import android.util.Log
import com.kovcom.konnect.Konnect
import com.kovcom.konnect.okhttp.interceptor.KonnectErrorInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpRequester(
    val konnect: Konnect
) {
    val client = OkHttpClient.Builder()
        .addInterceptor(KonnectErrorInterceptor(konnect::onError)).build()


    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        job = scope.launch {
            while (true) {
                try {
                    val request = Request.Builder().url("https://www.google.com").build()
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    Log.e("HttpRequester", "HTTP request failed: $e")
                }
                delay(5000)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}