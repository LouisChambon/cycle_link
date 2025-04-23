package com.example.cycle_link

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cycle_link.ui.theme.Cycle_linkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cycle_linkTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf("login") }
    var selectedBikeId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            "login" -> {
                LoginScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLoginClick = {
                        // TODO: Implémenter la logique de connexion
                        currentScreen = "home"
                    },
                    onRegisterClick = {
                        currentScreen = "register"
                    }
                )
            }
            "register" -> {
                RegisterScreen(
                    modifier = Modifier.padding(innerPadding),
                    onRegisterClick = {
                        // TODO: Implémenter la logique d'inscription
                        currentScreen = "login"
                    },
                    onLoginClick = {
                        currentScreen = "login"
                    }
                )
            }
            "home" -> {
                HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLogoutClick = {
                        currentScreen = "login"
                    },
                    onBikeClick = { bikeId ->
                        selectedBikeId = bikeId
                        currentScreen = "bike_detail"
                    }
                )
            }
            "bike_detail" -> {
                selectedBikeId?.let { bikeId ->
                    BikeDetailScreen(
                        bikeId = bikeId,
                        onBackClick = {
                            currentScreen = "home"
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Cycle_linkTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    Cycle_linkTheme {
        MainApp()
    }
}