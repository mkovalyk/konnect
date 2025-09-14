package com.kovcom.konnect.okhttp.strategy

import com.kovcom.konnect.core.strategy.PingStrategy
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OkHttpPingStrategy(
    private val host: String,
    private val timeoutMs: Long,
) : PingStrategy {

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun isHostReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().head().url("https://$host").build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }
}
