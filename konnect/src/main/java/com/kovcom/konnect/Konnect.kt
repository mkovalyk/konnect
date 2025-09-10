package com.example.myapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Enum representing the different states of network connectivity.
 */
enum class NetworkState {
    /** The network is connected and the target host is reachable. */
    REACHABLE,
    /** The network is connected, but the target host is not reachable. */
    UNAVAILABLE,
    /** There is no active network connection. */
    UNREACHABLE
}

/**
 * A class to monitor network state and ping Google's servers periodically.
 *
 * This class listens to network connectivity changes and app lifecycle events (foreground/background).
 * When the network is available AND the app is in the foreground, it starts a periodic
 * ping to "www.google.com" every 5 seconds. The pinging stops when either condition is no longer met.
 *
 * It provides a callback `onNetworkStateChanged` to listen for network status updates.
 *
 * @param context The application context to access system services.
 */
class Konnect(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null
    private val _lastStateFlow = MutableStateFlow<NetworkState?>(null)
    val lastStateFlow: StateFlow<NetworkState?> = _lastStateFlow.asStateFlow()

    /**
     * A callback to listen for changes in the network state.
     * Assign a lambda to this property to receive updates.
     * @Deprecated("Use lastStateFlow instead to observe network state changes.")
     */
    var onNetworkStateChanged: ((NetworkState) -> Unit)? = null

    private val isAppInForegroundFlow = MutableStateFlow(false)

    companion object {
        private const val TAG = "Konnect"
        private const val PING_INTERVAL_MS = 5000L
        private const val PING_HOST = "www.google.com"
        private const val PING_PORT = 80 // Port 80 for HTTP is generally open
        private const val PING_TIMEOUT_MS = 2000
    }

    /**
     * Observes the application's lifecycle to detect foreground/background transitions.
     */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Log.d(TAG, "App entered foreground.")
            isAppInForegroundFlow.value = true
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d(TAG, "App entered background.")
            isAppInForegroundFlow.value = false
        }
    }

    /**
     * A flow that emits the network availability status.
     */
    private val networkAvailabilityFlow: Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
                notifyStateChange(NetworkState.UNREACHABLE)
            }
        }

        // Get the initial network state
        val initialNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(initialNetwork)
        val isInitiallyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(isInitiallyConnected)

        // Register the callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Unregister the callback when the flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Only emit when the state actually changes

    /**
     * Starts listening for network and lifecycle changes.
     * This should be called once, typically from your Application's onCreate method.
     */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        // Combine the app state and network state flows to determine if we should be pinging.
        coroutineScope.launch {
            isAppInForegroundFlow.combine(networkAvailabilityFlow) { isForeground, isAvailable ->
                isForeground && isAvailable
            }.collect { shouldBePinging ->
                if (shouldBePinging) {
                    startPinging()
                } else {
                    stopPinging()
                }
            }
        }
        Log.i(TAG, "Konnect service started.")
    }

    /**
     * Stops listening and cleans up resources.
     */
    fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        coroutineScope.cancel("Konnect service is shutting down.")
        Log.i(TAG, "Konnect service stopped.")
    }

    /**
     * Notifies the listener about a network state change, avoiding redundant notifications.
     * The callback is always invoked on the main thread.
     * @param newState The new network state to report.
     */
    @Synchronized
    private fun notifyStateChange(newState: NetworkState) {
        if (_lastStateFlow.value != newState) {
            Log.i(TAG, "Network state changed from ${_lastStateFlow.value} to $newState")
            _lastStateFlow.value = newState
            // Ensure callback is invoked on the main thread for UI safety
            MainScope().launch {
                onNetworkStateChanged?.invoke(newState)
            }
        }
    }

    /**
     * Starts the periodic pinging coroutine if it's not already running.
     */
    private fun startPinging() {
        if (pingJob?.isActive == true) {
            Log.d(TAG, "Pinging is already active.")
            return
        }
        pingJob = coroutineScope.launch {
            Log.i(TAG, "Pinging started. Will ping $PING_HOST every ${PING_INTERVAL_MS}ms.")
            while (isActive) {
                val isReachable = isHostReachable()
                if (isReachable) {
                    notifyStateChange(NetworkState.REACHABLE)
                } else {
                    notifyStateChange(NetworkState.UNAVAILABLE)
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    /**
     * Stops the pinging coroutine if it is running.
     */
    private fun stopPinging() {
        if (pingJob?.isActive == true) {
            pingJob?.cancel()
            Log.i(TAG, "Pinging stopped.")
        }
        pingJob = null
    }

    /**
     * Performs the actual "ping" by attempting to open a socket connection.
     * This is a more reliable method than ICMP pings on Android.
     *
     * @return True if the host is reachable, false otherwise.
     */
    private suspend fun isHostReachable(): Boolean = withContext(Dispatchers.IO) {
        // We use a socket connection attempt as a proxy for a "ping".
        // It checks if we can establish a TCP connection to a given host and port.
        try {
            Log.d(TAG, "Pinging $PING_HOST...")
            Socket().use { socket ->
                socket.connect(InetSocketAddress(PING_HOST, PING_PORT), PING_TIMEOUT_MS)
                return@withContext true
            }
        } catch (e: IOException) {
            // An exception means we couldn't connect, so the host is considered unreachable.
            return@withContext false
        }
    }
}
