package com.kovcom.konnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapp.Konnect // Import Konnect
import com.example.myapp.NetworkState // Import NetworkState
import com.kovcom.konnect.ui.theme.KonnectTheme

class MainActivity : ComponentActivity() {

    private lateinit var konnect: Konnect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        konnect = Konnect(applicationContext)
        konnect.start()

        setContent {
            KonnectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val networkState by konnect.lastStateFlow.collectAsState()
                    Greeting(
                        name = "Android",
                        networkState = networkState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, networkState: NetworkState?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "Hello $name!\nNetwork State: ${networkState?.name ?: "Initializing..."}"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KonnectTheme {
        Greeting("Android", NetworkState.REACHABLE)
    }
}
