package com.example.cycle_link.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit
) {
    /* ---------- Firebase ---------- */
    val auth             = FirebaseAuth.getInstance()
    val user             = auth.currentUser
    val firestore        = FirebaseFirestore.getInstance()
    val context          = LocalContext.current
    val scope            = rememberCoroutineScope()

    /* ---------- États user ---------- */
    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    /* ---------- Édition profil ---------- */
    var isEditing   by remember { mutableStateOf(false) }
    var editName    by remember { mutableStateOf("") }
    var editEmail   by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }

    /* ---------- Changer mot de passe ---------- */
    var showPwdDialog      by remember { mutableStateOf(false) }
    var currentPwd         by remember { mutableStateOf("") }
    var newPwd             by remember { mutableStateOf("") }
    var confirmNewPwd      by remember { mutableStateOf("") }
    var pwdMsg             by remember { mutableStateOf<String?>(null) }

    /* ---------- Listener Firestore ---------- */
    DisposableEffect(user?.uid) {
        val reg = user?.uid?.let { uid ->
            firestore.collection("users")
                .document(uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) { isLoading = false ; return@addSnapshotListener }
                    if (snap != null && snap.exists()) {
                        name  = snap.getString("name").orEmpty()
                        email = user.email ?: ""
                        role  = snap.getString("role")
                        editName  = name
                        editEmail = email
                    }
                    isLoading = false
                }
        }
        onDispose { reg?.remove() }
    }

    /* ===================== UI ===================== */
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
                            isLoading = true
                            user?.uid?.let { uid ->
                                firestore.collection("users")
                                    .document(uid)
                                    .update("name", editName)
                                    .addOnSuccessListener {
                                        if (email != editEmail) { showEmailDialog = true }
                                        else {
                                            isEditing = false ; isLoading = false
                                            Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }) { Icon(Icons.Default.Save, contentDescription = "Enregistrer") }
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Se déconnecter")
                    }
                }
            )
        }
    ) { inner ->
        /* ---------- Corps ---------- */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) { CircularProgressIndicator() ; return@Column }

            /* ----- Avatar / nom / email ----- */
            Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editName, onValueChange = { editName = it },
                    label = { Text("Nom") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editEmail, onValueChange = { editEmail = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(name.ifBlank { "— Nom non défini —" },
                    style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(email, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))
            Text("Rôle :", style = MaterialTheme.typography.titleMedium)
            Text(role ?: "—", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            /* ---------- Bouton changer mot de passe ---------- */
            Button(
                onClick = { showPwdDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Changer le mot de passe") }

            if (isEditing) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { isEditing = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Annuler") }
            }
        }
    }

    /* ===================== Dialog Changer email ===================== */
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false ; isLoading = false },
            title = { Text("Changer l'email") },
            text  = { Text("Vous devrez vous reconnecter après le changement. Continuer ?") },
            confirmButton = {
                Button(onClick = {
                    showEmailDialog = false
                    user?.updateEmail(editEmail)
                        ?.addOnSuccessListener {
                            isEditing = false ; isLoading = false
                            Toast.makeText(context, "Email mis à jour, veuillez vous reconnecter", Toast.LENGTH_LONG).show()
                            auth.signOut() ; onLogoutClick()
                        }
                        ?.addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }) { Text("Continuer") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEmailDialog = false ; editEmail = email
                    isEditing = false ; isLoading = false
                }) { Text("Annuler") }
            }
        )
    }

    /* ===================== Dialog Changer mot de passe ===================== */
    if (showPwdDialog) {
        AlertDialog(
            onDismissRequest = {
                showPwdDialog = false
                currentPwd = "" ; newPwd = "" ; confirmNewPwd = "" ; pwdMsg = null
            },
            title = { Text("Changer votre mot de passe") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPwd, onValueChange = { currentPwd = it },
                        label = { Text("Mot de passe actuel") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPwd, onValueChange = { newPwd = it },
                        label = { Text("Nouveau mot de passe") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPwd, onValueChange = { confirmNewPwd = it },
                        label = { Text("Confirmer le nouveau mot de passe") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    pwdMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    /* ---- validation ---- */
                    if (currentPwd.isBlank() || newPwd.isBlank() || confirmNewPwd.isBlank()) {
                        pwdMsg = "Tous les champs sont requis"
                        return@Button
                    }
                    if (newPwd != confirmNewPwd) {
                        pwdMsg = "Les mots de passe ne correspondent pas"
                        return@Button
                    }
                    /* ---- update ---- */
                    user?.let { u ->
                        val credential = EmailAuthProvider
                            .getCredential(u.email ?: "", currentPwd)
                        scope.launch {
                            pwdMsg = "Mise à jour..."
                            u.reauthenticate(credential)
                                .addOnSuccessListener {
                                    u.updatePassword(newPwd)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Mot de passe changé !", Toast.LENGTH_LONG).show()
                                            showPwdDialog = false
                                            auth.signOut() ; onLogoutClick()
                                        }
                                        .addOnFailureListener { e ->
                                            pwdMsg = "Erreur : ${e.message}"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    pwdMsg = "Mot de passe actuel incorrect"
                                }
                        }
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPwdDialog = false
                    currentPwd = "" ; newPwd = "" ; confirmNewPwd = "" ; pwdMsg = null
                }) { Text("Annuler") }
            }
        )
    }
}
