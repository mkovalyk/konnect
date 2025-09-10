package com.kovcom.konnect.ping.strategy

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocketPingStrategy(
    private val host: String,
    private val port: Int,
    private val timeoutMs: Int
) : PingStrategy {

    override suspend fun isHostReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: IOException) {
            false
        }
    }
}
