package com.kovcom.konnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kovcom.konnect.ping.strategy.OkHttpPingStrategy
import com.kovcom.konnect.ping.strategy.SocketPingStrategy
import com.kovcom.konnect.ui.theme.KonnectTheme

class MainActivity : ComponentActivity() {

    private var konnect: Konnect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KonnectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NetworkMonitorScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStrategyChanged = { strategy ->
                            // Stop previous Konnect instance
                            konnect?.stop()

                            // Create new Konnect instance with selected strategy
                            konnect = Konnect(applicationContext, strategy)
                            konnect?.start()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkMonitorScreen(
    modifier: Modifier = Modifier,
    onStrategyChanged: (strategy: com.kovcom.konnect.ping.strategy.PingStrategy) -> Unit
) {
    var selectedStrategy by remember { mutableStateOf("OkHttp") }
    var konnectInstance by remember { mutableStateOf<Konnect?>(null) }
    val context = LocalContext.current

    // Create initial Konnect instance
    LaunchedEffect(Unit) {
        val initialStrategy = OkHttpPingStrategy("www.google.com", 5000L)
        onStrategyChanged(initialStrategy)
        konnectInstance = Konnect(context, initialStrategy)
    }

    // Recreate Konnect when strategy changes
    LaunchedEffect(selectedStrategy) {
        konnectInstance?.stop()

        val strategy = when (selectedStrategy) {
            "OkHttp" -> OkHttpPingStrategy("www.google.com", 5000L)
            "Socket" -> SocketPingStrategy("www.google.com", 80, 5000)
            else -> OkHttpPingStrategy("www.google.com", 5000L)
        }

        onStrategyChanged(strategy)
        konnectInstance = Konnect(context, strategy).apply { start() }
    }

    val networkState by konnectInstance?.lastStateFlow?.collectAsState() ?: remember { mutableStateOf(null) }

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

        Spacer(modifier = Modifier.height(32.dp))

        // Network state display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Strategy: $selectedStrategy",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Network State: ${networkState?.name ?: "Initializing..."}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkMonitorScreenPreview() {
    KonnectTheme {
        NetworkMonitorScreen(onStrategyChanged = {})
    }
}
