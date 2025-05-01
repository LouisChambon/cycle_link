package com.example.cycle_link.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast

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
    val context   = LocalContext.current

    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Edit states
    var isEditing by remember { mutableStateOf(false) }
    var editName  by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }

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
                        email = user.email ?: ""
                        role = snap.getString("role")
                        
                        // Initialize edit fields
                        editName = name
                        editEmail = email
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
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                    } else {
                        IconButton(onClick = { 
                            // Save user profile
                            isLoading = true
                            user?.uid?.let { uid ->
                                // Update name in Firestore
                                firestore.collection("users")
                                    .document(uid)
                                    .update("name", editName)
                                    .addOnSuccessListener {
                                        // If email changed, update email in auth
                                        if (email != editEmail) {
                                            showEmailDialog = true
                                        } else {
                                            isEditing = false
                                            isLoading = false
                                            Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Enregistrer")
                        }
                    }
                    
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
            
            if (showEmailDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        showEmailDialog = false
                        isLoading = false
                    },
                    title = { Text("Changer l'email") },
                    text = { Text("Pour changer votre adresse email, vous devez vous reconnecter. Voulez-vous continuer?") },
                    confirmButton = {
                        Button(onClick = {
                            showEmailDialog = false
                            user?.updateEmail(editEmail)
                                ?.addOnSuccessListener {
                                    isEditing = false
                                    isLoading = false
                                    Toast.makeText(context, "Email mis à jour, veuillez vous reconnecter", Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                    onLogoutClick()
                                }
                                ?.addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }) {
                            Text("Continuer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { 
                            showEmailDialog = false
                            editEmail = email
                            isEditing = false
                            isLoading = false
                        }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(name.ifBlank { "— Nom non défini —" },
                    style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))

            Text("Rôle :", style = MaterialTheme.typography.titleMedium)
            Text(role ?: "—", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            Button(onClick = onChangePasswordClick, modifier = Modifier.fillMaxWidth()) {
                Text("Changer le mot de passe")
            }
            
            if (isEditing) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { isEditing = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Annuler")
                }
            }
        }
    }
}
