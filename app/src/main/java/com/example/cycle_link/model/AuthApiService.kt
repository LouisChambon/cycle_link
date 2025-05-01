package com.example.cycle_link.model

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val address: String
)
data class AuthResponse(
    val _id: String,
    val name: String,
    val email: String,
    val token: String
)

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/signup")
    suspend fun signup(@Body body: SignupRequest): AuthResponse
}
