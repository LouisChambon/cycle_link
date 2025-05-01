package com.example.cycle_link.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycle_link.model.UserDto
import com.example.cycle_link.model.UserRepository
import com.example.cycle_link.ui.theme.Cycle_linkTheme
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    val auth      = FirebaseAuth.getInstance()
    val user      = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()

    var name      by remember { mutableStateOf<String>("") }
    var role      by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(user?.uid) {
        val reg = user?.uid?.let { uid ->
            firestore.collection("users")
                .document(uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    if (snap != null && snap.exists()) {
                        name = snap.getString("name").orEmpty()
                        role = snap.getString("role")
                    }
                    isLoading = false
                }
        }
        onDispose { reg?.remove() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar   = {
            TopAppBar(
                title = { Text("Mon profil") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Se déconnecter")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier           = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))

            // Affiche le nom et l’email
            Text(name.ifBlank { "— Nom non défini —" },
                style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(user?.email.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // Affiche le rôle
            Text("Rôle :", style = MaterialTheme.typography.titleMedium)
            Text(role ?: "—", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            Button(onClick = onChangePasswordClick, modifier = Modifier.fillMaxWidth()) {
                Text("Changer le mot de passe")
            }
        }
    }
}
