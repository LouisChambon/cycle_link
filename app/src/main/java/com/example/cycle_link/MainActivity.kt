package com.example.cycle_link

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private lateinit var oneTapClient : SignInClient
    private lateinit var oneTapRequest: BeginSignInRequest
    private lateinit var googleClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oneTapClient = Identity.getSignInClient(this)
        oneTapRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        /* -------- Fallback GoogleSignIn ---------- */
        googleClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        enableEdgeToEdge()
        setContent {
            Cycle_linkTheme {
                MainApp(oneTapClient, oneTapRequest, googleClient)
            }
        }
    }
}

/* **************************************************************** */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    oneTapClient : SignInClient,
    oneTapRequest: BeginSignInRequest,
    googleClient : GoogleSignInClient
) {
    val auth      = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context   = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    val onLoggedIn = { currentScreen = Screen.Home }

    /* ---------- launchers ---------- */
    val oneTapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            handleGoogleResult(res.data, auth, firestore, context, onLoggedIn)
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            handleGoogleResult(res.data, auth, firestore, context, onLoggedIn)
        }
    }

    /** Lance One-Tap d’abord, sinon Google Sign-In classique. */
    fun startGoogleFlow() {
        oneTapClient.beginSignIn(oneTapRequest)
            .addOnSuccessListener { result ->
                val req = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                oneTapLauncher.launch(req)
            }
            .addOnFailureListener {
                googleLauncher.launch(googleClient.signInIntent)
            }
    }

    /* ---------- navigation ---------- */
    var selectedBikeId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen in bottomTabs) {
                BottomBar(currentScreen.route) { route ->
                    currentScreen = bottomTabs.first { it.route == route }
                }
            }
        }
    ) { inner ->

        when (currentScreen) {
            /* ------- Auth ------- */
            Screen.Login -> LoginScreen(
                modifier = Modifier.padding(inner),
                onLoginClick = { mail, pwd ->
                    auth.signInWithEmailAndPassword(mail, pwd)
                        .addOnSuccessListener { currentScreen = Screen.Home }
                        .addOnFailureListener { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                },
                onRegisterClick = { currentScreen = Screen.Register },
                onGoogleClick   = { startGoogleFlow() }
            )

            Screen.Register -> RegisterScreen(
                modifier = Modifier.padding(inner),
                onRegisterClick = { name, mail, pwd ->
                    auth.createUserWithEmailAndPassword(mail, pwd)
                        .addOnSuccessListener { res ->
                            firestore.collection("users").document(res.user!!.uid)
                                .set(
                                    mapOf(
                                        "name"      to name,
                                        "email"     to mail,
                                        "role"      to "customer",
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            currentScreen = Screen.Home
                        }
                        .addOnFailureListener { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                },
                onLoginClick  = { currentScreen = Screen.Login },
                onGoogleClick = { startGoogleFlow() }
            )

            /* ------- reste identique (home / publish / fav / …) ------- */
            Screen.Home -> HomeScreen(
                modifier = Modifier.padding(inner),
                onBikeClick = { id -> selectedBikeId = id; currentScreen = Screen.BikeDetail }
            )

            Screen.Publish -> PublishScreen(
                modifier = Modifier.padding(inner),
                onPublished = { currentScreen = Screen.Home }
            )

            Screen.Favorites -> FavoritesScreen(
                modifier = Modifier.padding(inner),
                onBikeClick = { id -> selectedBikeId = id; currentScreen = Screen.BikeDetail }
            )

            Screen.Profile -> ProfileScreen(
                modifier = Modifier.padding(inner),
                onLogoutClick = { auth.signOut(); currentScreen = Screen.Login },
                onChangePasswordClick = {}
            )

            Screen.BikeDetail -> selectedBikeId?.let { id ->
                BikeDetailScreen(
                    bikeId = id,
                    onBackClick = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(inner)
                )
            }
        }
    }
}

/* --------- Utilitaire commun (One-Tap OU GoogleSignIn) --------- */
private fun handleGoogleResult(
    intent     : Intent?,
    auth       : FirebaseAuth,
    firestore  : FirebaseFirestore,
    context    : android.content.Context,
    onLoggedIn : () -> Unit
) {
    try {
        /* 1) One-Tap ? */
        val cred: SignInCredential? =
            try { Identity.getSignInClient(context).getSignInCredentialFromIntent(intent) }
            catch (_: Exception) { null }

        val idToken = cred?.googleIdToken
            ?: GoogleSignIn.getSignedInAccountFromIntent(intent)
                .getResult(ApiException::class.java)
                ?.idToken

        if (idToken == null) return

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnSuccessListener { res ->
                val uid   = res.user!!.uid
                val data  = mapOf(
                    "name"      to (res.user?.displayName ?: ""),
                    "email"     to (res.user?.email ?: ""),
                    "role"      to "customer",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .addOnFailureListener { e ->
                        Log.e("GoogleAuth", "❌ impossible d'écrire le doc user", e)
                    }

                Toast.makeText(context, "Connecté !", Toast.LENGTH_SHORT).show()
                onLoggedIn()
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }

    } catch (e: Exception) {
        Log.e("GoogleAuth", "Erreur Google Sign-In", e)
    }
}
