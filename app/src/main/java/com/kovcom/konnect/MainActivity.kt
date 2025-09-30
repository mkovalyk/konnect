package com.kovcom.konnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kovcom.konnect.core.logger.AndroidLogger
import com.kovcom.konnect.core.strategy.SocketPingStrategy
import com.kovcom.konnect.okhttp.strategy.OkHttpPingStrategy
import com.kovcom.konnect.ui.theme.KonnectTheme
import java.net.UnknownHostException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KonnectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NetworkMonitorScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkMonitorScreen(modifier: Modifier = Modifier) {
    var selectedStrategy by remember { mutableStateOf("OkHttp") }
    var konnectInstance by remember { mutableStateOf<Konnect?>(null) }
    val context = LocalContext.current

    // This effect manages the lifecycle of the Konnect instance.
    // It runs when the composable enters the composition and whenever `selectedStrategy` changes.
    DisposableEffect(selectedStrategy) {
        // Create a new strategy instance based on the selection
        val strategy = when (selectedStrategy) {
            "OkHttp" -> OkHttpPingStrategy("www.google.com", 5000L)
            "Socket" -> SocketPingStrategy("www.google.com", 80, 5000)
            else -> OkHttpPingStrategy("www.google.com", 5000L) // Default
        }

        // Create and start the new Konnect instance using the Builder
        val newKonnectInstance = Konnect.Builder(context, strategy)
            .setLogger(AndroidLogger()) // Set the logger using the Builder
            .build()
            .apply { start() }
        konnectInstance = newKonnectInstance

        // The onDispose block is called when the effect leaves the composition
        // (e.g., screen rotation, new strategy selected, or navigating away).
        onDispose {
            newKonnectInstance.stop()
        }
    }

    val networkState by konnectInstance?.lastStateFlow?.collectAsState()
        ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Ping Strategy",
            style = MaterialTheme.typography.headlineMedium
        )

        // Radio button group for strategy selection
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedStrategy == "OkHttp"),
                        onClick = { selectedStrategy = "OkHttp" }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedStrategy == "OkHttp"),
                    onClick = { selectedStrategy = "OkHttp" }
                )
                Text(
                    text = "OkHttp Strategy (HTTP)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedStrategy == "Socket"),
                        onClick = { selectedStrategy = "Socket" }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedStrategy == "Socket"),
                    onClick = { selectedStrategy = "Socket" }
                )
                Text(
                    text = "Socket Strategy (TCP)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network state display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current Strategy: $selectedStrategy",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Network State: ${networkState?.name ?: "Initializing..."}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to trigger onError
        Button(
            onClick = {
                konnectInstance?.onError(UnknownHostException("Simulated error by button press"))
            },
            enabled = konnectInstance != null
        ) {
            Text("Simulate Network Error")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkMonitorScreenPreview() {
    KonnectTheme {
        NetworkMonitorScreen()
    }
}
