package com.kovcom.konnect

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kovcom.konnect.logger.AndroidLogger
import com.kovcom.konnect.logger.Logger
import com.kovcom.konnect.ping.strategy.PingStrategy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
 * @param pingStrategy The strategy to use for pinging a host.
 * @param pingIntervalMs The interval in milliseconds between pings.
 * @param logger The logger to use for logging messages.
 */
class Konnect private constructor(
    context: Context,
    private val pingStrategy: PingStrategy,
    private val pingIntervalMs: Long,
    private val logger: Logger
) : Closeable {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null
    private val _lastStateFlow = MutableStateFlow<NetworkState?>(null)
    val lastStateFlow: StateFlow<NetworkState?> = _lastStateFlow.asStateFlow()

    private val isAppInForegroundFlow = MutableStateFlow(false)

    companion object {
        private const val TAG = "Konnect"
    }

    /**
     * Observes the application's lifecycle to detect foreground/background transitions.
     */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            logger.d(TAG, "App entered foreground.")
            isAppInForegroundFlow.value = true
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            logger.d(TAG, "App entered background.")
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
        val isInitiallyConnected =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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
        logger.i(TAG, "Konnect service started.")
    }

    /**
     * Stops listening and cleans up resources.
     */
    fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        coroutineScope.cancel("Konnect service is shutting down.")
        close()
        logger.i(TAG, "Konnect service stopped.")
    }

    override fun close() {
        (pingStrategy as? Closeable)?.close()
    }

    /**
     * Notifies the listener about a network state change, avoiding redundant notifications.
     * The callback is always invoked on the main thread.
     * @param newState The new network state to report.
     */
    @Synchronized
    private fun notifyStateChange(newState: NetworkState) {
        if (_lastStateFlow.value != newState) {
            logger.i(TAG, "Network state changed from ${_lastStateFlow.value} to $newState")
            _lastStateFlow.value = newState
        }
    }

    /**
     * Starts the periodic pinging coroutine if it's not already running.
     */
    private fun startPinging() {
        if (pingJob?.isActive == true) {
            logger.d(TAG, "Pinging is already active.")
            return
        }
        pingJob = coroutineScope.launch {
            logger.i(TAG, "Pinging started. Will ping every ${pingIntervalMs}ms.")
            while (isActive) {
                val isReachable = pingStrategy.isHostReachable()
                if (isReachable) {
                    notifyStateChange(NetworkState.REACHABLE)
                } else {
                    notifyStateChange(NetworkState.UNAVAILABLE)
                }
                delay(pingIntervalMs)
            }
        }
    }

    /**
     * Stops the pinging coroutine if it is running.
     */
    private fun stopPinging() {
        if (pingJob?.isActive == true) {
            pingJob?.cancel()
            logger.i(TAG, "Pinging stopped.")
        }
        pingJob = null
    }

    /**
     * Triggers an immediate ping request. This can be used to re-evaluate network state
     * immediately after an error or a change that might affect reachability.
     * @param cause The throwable that caused the error, for logging purposes.
     */
    fun onError(cause: Throwable) {
        if (cause is SocketTimeoutException || cause is UnknownHostException) {
            stopPinging()
            logger.e(
                TAG,
                "Network-related error occurred, triggering immediate ping. Cause: ${cause.message}"
            )
            startPinging()
        } else {
            logger.e(TAG, "Non-network error occurred. Cause: ${cause.message}", cause)
        }
    }

    /**
     * A builder for creating instances of [Konnect].
     * @param context The application context.
     * @param pingStrategy The strategy to use for pinging.
     */
    class Builder(
        private val context: Context,
        private val pingStrategy: PingStrategy
    ) {
        private var pingIntervalMs: Long = 5000L
        private var logger: Logger = AndroidLogger()

        /**
         * Sets the interval in milliseconds between pings.
         * @param interval The ping interval.
         * @return The builder instance.
         */
        fun setPingInterval(interval: Long): Builder {
            this.pingIntervalMs = interval
            return this
        }

        /**
         * Sets the logger to be used by Konnect.
         * @param logger The logger instance.
         * @return The builder instance.
         */
        fun setLogger(logger: Logger): Builder {
            this.logger = logger
            return this
        }

        /**
         * Builds and returns a [Konnect] instance with the configured parameters.
         * @return A new [Konnect] instance.
         */
        fun build(): Konnect {
            return Konnect(
                context = context,
                pingStrategy = pingStrategy,
                pingIntervalMs = pingIntervalMs,
                logger = logger
            )
        }
    }
}
