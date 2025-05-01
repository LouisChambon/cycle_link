package com.example.cycle_link

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.cycle_link.navigation.*
import com.example.cycle_link.screens.*
import com.example.cycle_link.ui.theme.Cycle_linkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
    val auth      = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context   = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var selectedBikeId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier  = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen in bottomTabs) {
                BottomBar(
                    currentRoute  = currentScreen.route,
                    onTabSelected = { route ->
                        currentScreen = bottomTabs.first { it.route == route }
                    }
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Login -> LoginScreen(
                modifier       = Modifier.padding(innerPadding),
                onLoginClick   = { email, pwd ->
                    auth.signInWithEmailAndPassword(email, pwd)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Connecté !", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.Home
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erreur connexion : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                onRegisterClick = { currentScreen = Screen.Register }
            )

            Screen.Register -> RegisterScreen(
                modifier         = Modifier.padding(innerPadding),
                onRegisterClick  = { name, email, pwd ->
                    auth.createUserWithEmailAndPassword(email, pwd)
                        .addOnSuccessListener { authRes ->
                            val uid  = authRes.user!!.uid
                            val data = mapOf(
                                "email"     to email,
                                "name"      to name,
                                "role"      to "customer",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            firestore.collection("users")
                                .document(uid)
                                .set(data)
                                .addOnSuccessListener {
                                    Log.d("Register", "✅ user doc créé pour $uid")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Register", "❌ erreur création doc user", e)
                                }
                            Toast.makeText(context, "Compte créé !", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.Home
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erreur inscription : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                onLoginClick = { currentScreen = Screen.Login }
            )

            Screen.Home -> HomeScreen(
                modifier    = Modifier.padding(innerPadding),
                onBikeClick = { id ->
                    selectedBikeId = id
                    currentScreen  = Screen.BikeDetail
                }
            )

            Screen.Publish -> PublishScreen(
                modifier    = Modifier.padding(innerPadding),
                onPublished = {
                    Toast.makeText(context, "Annonce publiée !", Toast.LENGTH_SHORT).show()
                    currentScreen = Screen.Home
                }
            )

            Screen.Favorites -> FavoritesScreen(
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Profile -> ProfileScreen(
                modifier              = Modifier.padding(innerPadding),
                onLogoutClick         = {
                    auth.signOut()
                    currentScreen = Screen.Login
                },
                onChangePasswordClick = {
                    // TODO: Naviguer vers un écran de changement de mot de passe
                }
            )

            Screen.BikeDetail -> selectedBikeId?.let { id ->
                BikeDetailScreen(
                    bikeId      = id,
                    onBackClick = { currentScreen = Screen.Home },
                    modifier    = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
