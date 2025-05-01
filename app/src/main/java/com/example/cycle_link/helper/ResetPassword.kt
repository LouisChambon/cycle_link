package com.example.cycle_link.helper

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

suspend fun sendResetEmail(email: String): String {
    return try {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
        "Un e-mail de réinitialisation a été envoyé à $email"
    } catch (e: Exception) {
        "Erreur : ${e.localizedMessage}"
    }
}
