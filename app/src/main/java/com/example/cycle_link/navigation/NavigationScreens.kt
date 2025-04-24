package com.example.cycle_link.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cycle_link.ui.theme.Cycle_linkTheme
import com.example.cycle_link.R
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.res.painterResource
import com.example.cycle_link.screens.FavoritesScreen
import com.example.cycle_link.screens.ProfileScreen
import com.example.cycle_link.screens.PublishScreen

sealed class Screen(val route: String, val iconRes: Int, val label: String) {
    object Home      : Screen("home",      R.drawable.ic_home,     "Accueil")
    object Publish   : Screen("publish",   R.drawable.ic_add,      "Publier")
    object Favorites : Screen("favorites", R.drawable.ic_favorite, "Favoris")
    object Profile   : Screen("profile",   R.drawable.ic_profile,  "Profil")
    object BikeDetail: Screen("bike_detail", 0,                 "")
    object Login     : Screen("login",       0,                 "")
    object Register  : Screen("register",    0,                 "")
}

val bottomTabs = listOf(
    Screen.Home,
    Screen.Publish,
    Screen.Favorites,
    Screen.Profile
)

@Composable
fun BottomBar(currentRoute: String, onTabSelected: (String) -> Unit) {
    NavigationBar {
        bottomTabs.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = screen.label
                    )
                },
                label = { androidx.compose.material3.Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = { onTabSelected(screen.route) }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NavigationScreensPreview() {
    Cycle_linkTheme {
        androidx.compose.foundation.layout.Column {
            PublishScreen(modifier = Modifier.weight(1f))
            FavoritesScreen(modifier = Modifier.weight(1f))
            ProfileScreen(modifier = Modifier.weight(1f))
        }
    }
}
