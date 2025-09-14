package com.kovcom.konnect.core.strategy

 
interface PingStrategy { 
 suspend fun isHostReachable(): Boolean
 }