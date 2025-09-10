package com.kovcom.konnect.ping.strategy

interface PingStrategy {
    /**
     * Performs a ping request to check if the host is reachable.
     * @return True if the host is reachable, false otherwise.
     */
    suspend fun isHostReachable(): Boolean
}
