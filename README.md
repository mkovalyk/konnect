
# Konnect - Network Monitoring Library

Konnect is an Android library designed for monitoring network connectivity and host reachability using various strategies.
It provides a flexible way to check network status, handle errors, and integrate with different ping mechanisms.

## Module Structure

Konnect is divided into two main modules:

1.  **`konnect-core`**: Contains the core logic for network monitoring, including the `Konnect` class, `PingStrategy` interface, `SocketPingStrategy` implementation, and the `Logger` abstraction.
2.  **`konnect-okhttp`**: Provides an `OkHttpPingStrategy` implementation that leverages OkHttp for HTTP-based pings and an `KonnectErrorInterceptor` to integrate network error handling with the `Konnect` instance.

## Setup

To use Konnect in your Android project, add the following dependencies to your `app/build.gradle.kts` file (or the `build.gradle.kts` of any module that needs to use Konnect):

```kotlin
// For core functionality (Konnect class, SocketPingStrategy, Logger)
implementation(project(":konnect-core"))

// For OkHttp-based ping strategy and interceptor
implementation(project(":konnect-okhttp"))
// Also include OkHttp dependency
implementation("com.squareup.okhttp3:okhttp:4.12.0") // Use your desired OkHttp version
```

### `settings.gradle.kts`

Ensure your `settings.gradle.kts` includes the new modules:

```kotlin
include(":app", ":konnect-core", ":konnect-okhttp")
```

## Usage

### 1. Basic Konnect Initialization (with SocketPingStrategy)

To start monitoring, you create a `Konnect` instance using its `Builder` and a `PingStrategy`. Here's how to use the `SocketPingStrategy`:

```kotlin
val context = LocalContext.current
var konnectInstance by remember { mutableStateOf<Konnect?>(null) }

DisposableEffect(Unit) {
    val newKonnectInstance = Konnect.Builder(context)
        .setPingInterval(5000L) // Set ping interval (e.g., every 5 seconds)
        .setLogger(AndroidLogger()) // Use default Android logger
        .setPingStrategyFactory { onErrorAction ->
            // Provide SocketPingStrategy
            SocketPingStrategy("www.google.com", 80, 5000)
        }
        .build()
        .apply { start() }

    konnectInstance = newKonnectInstance

    onDispose {
        newKonnectInstance.stop()
    }
}

val networkState by konnectInstance?.lastStateFlow?.collectAsState() ?: remember { mutableStateOf(null) }
// ... Display networkState
```

### 2. Using OkHttpPingStrategy with Error Interceptor

For HTTP-based pings, use `OkHttpPingStrategy`. To automatically integrate network errors from OkHttp into Konnect's error handling, you'll use the `KonnectErrorInterceptor`.

```kotlin
val context = LocalContext.current
var konnectInstance by remember { mutableStateOf<Konnect?>(null) }

DisposableEffect(Unit) {
    val newKonnectInstance = Konnect.Builder(context)
        .setPingInterval(5000L)
        .setLogger(AndroidLogger())
        .setPingStrategyFactory { onErrorAction ->
            // Provide OkHttpPingStrategy with the KonnectErrorInterceptor
            OkHttpPingStrategy("https://www.google.com", 5000L, onErrorAction)
        }
        .build()
        .apply { start() }

    konnectInstance = newKonnectInstance

    onDispose {
        newKonnectInstance.stop()
    }
}

val networkState by konnectInstance?.lastStateFlow?.collectAsState() ?: remember { mutableStateOf(null) }
// ... Display networkState
```

**Note**: The `OkHttpPingStrategy` constructor now takes an `onErrorAction: (Throwable) -> Unit` which it passes to the `KonnectErrorInterceptor` internally.

### 3. Custom Logger

You can provide your own implementation of the `Logger` interface to `Konnect.Builder`:

```kotlin
// Define your custom logger
class MyCustomLogger : Logger {
    override fun d(tag: String, message: String) { /* Log to your analytics, etc. */ }
    override fun e(tag: String, message: String, throwable: Throwable?) { /* Handle errors */ }
}

// ... in your Konnect Builder
.setLogger(MyCustomLogger())
// ...
```

### 4. Handling Errors (`konnectInstance.onError(Throwable)`) 

You can manually trigger Konnect's error handling (which can initiate an immediate re-ping if the exception is network-related) by calling `onError`:

```kotlin
Button(
    onClick = {
        konnectInstance?.onError(UnknownHostException("Simulated error by button press"))
    },
    enabled = konnectInstance != null
) {
    Text("Simulate Network Error")
}
```

Konnect is configured to filter specific network-related exceptions (like `SocketTimeoutException` or `UnknownHostException`) in its `onError` method to trigger an immediate re-ping, while other exceptions are simply logged.

Remember to stop the Konnect instance when it's no longer needed (e.g., in `onDispose` for Composables or `onDestroy` for Activities) to prevent memory leaks and unnecessary network activity.
