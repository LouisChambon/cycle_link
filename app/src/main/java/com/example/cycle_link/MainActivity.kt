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
import com.example.cycle_link.screens.HomeScreen
import com.example.cycle_link.screens.LoginScreen
import com.example.cycle_link.screens.RegisterScreen
import com.example.cycle_link.ui.theme.Cycle_linkTheme
import com.example.cycle_link.navigation.BottomBar
import com.example.cycle_link.navigation.Screen
import com.example.cycle_link.navigation.bottomTabs
import com.example.cycle_link.screens.ProfileScreen
import com.example.cycle_link.screens.PublishScreen
import com.example.cycle_link.screens.FavoritesScreen

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var selectedBikeId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen in bottomTabs) {
                BottomBar(
                    currentRoute = currentScreen.route,
                    onTabSelected = { route ->
                        currentScreen = bottomTabs.first { it.route == route }
                    }
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Login -> LoginScreen(
                modifier = Modifier.padding(innerPadding),
                onLoginClick    = { currentScreen = Screen.Home },
                onRegisterClick = { currentScreen = Screen.Register }
            )
            Screen.Register -> RegisterScreen(
                modifier = Modifier.padding(innerPadding),
                onRegisterClick = { currentScreen = Screen.Login },
                onLoginClick    = { currentScreen = Screen.Login }
            )

            Screen.Home -> HomeScreen(
                modifier      = Modifier.padding(innerPadding),
                onBikeClick   = { id ->
                    selectedBikeId = id
                    currentScreen = Screen.BikeDetail
                }
            )
            Screen.Publish   -> PublishScreen(modifier = Modifier.padding(innerPadding))
            Screen.Favorites -> FavoritesScreen(modifier = Modifier.padding(innerPadding))
            Screen.Profile   -> ProfileScreen(modifier = Modifier.padding(innerPadding))

            Screen.BikeDetail -> selectedBikeId?.let { id ->
                BikeDetailScreen(
                    bikeId     = id,
                    onBackClick = { currentScreen = Screen.Home },
                    modifier    = Modifier.padding(innerPadding)
                )
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