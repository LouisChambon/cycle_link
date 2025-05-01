package com.example.cycle_link.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycle_link.components.GoogleSignInButton
import com.example.cycle_link.helper.sendResetEmail
import com.example.cycle_link.ui.theme.Cycle_linkTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onGoogleClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    /* ---------- États formulaire connexion ---------- */
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    /* ---------- États réinitialisation mot de passe ---------- */
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        /* ----------- Titre ----------- */
        Text(
            text = "Connexion",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        /* ----------- Email ----------- */
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = showError && email.isBlank(),
            supportingText = {
                if (showError && email.isBlank()) {
                    Text("L'email est requis")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        /* -------- Mot de passe -------- */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            isError = showError && password.isBlank(),
            supportingText = {
                if (showError && password.isBlank()) {
                    Text("Le mot de passe est requis")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        /* ----------- Bouton connexion ----------- */
        Button(
            onClick = {
                showError = true
                if (email.isNotBlank() && password.isNotBlank()) {
                    onLoginClick(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Se connecter")
        }

        /* -------- Google Sign-In -------- */
        Spacer(Modifier.height(12.dp))
        GoogleSignInButton(onClick = onGoogleClick)

        /* -------- Mot de passe oublié -------- */
        TextButton(onClick = { showResetDialog = true }) {
            Text("Mot de passe oublié ?")
        }

        /* -------- Lien inscription -------- */
        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Pas encore de compte ? S'inscrire")
        }
    }

    /* ============================================================
       Dialog réinitialisation mot de passe
       ============================================================ */
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetEmail = ""
                resetMsg = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            scope.launch {
                                resetMsg = sendResetEmail(resetEmail)
                            }
                        }
                    }
                ) { Text("Envoyer") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            },
            title = { Text("Réinitialiser le mot de passe") },
            text = {
                Column {
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Votre e-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    resetMsg?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        )
    }
}

/* -------- Preview -------- */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Cycle_linkTheme {
        LoginScreen()
    }
}
