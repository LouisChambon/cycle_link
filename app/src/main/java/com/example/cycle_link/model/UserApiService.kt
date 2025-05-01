package com.example.cycle_link.model

import retrofit2.http.GET
import retrofit2.http.Header

interface UserApiService {
    @GET("users/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String
    ): UserDto
}
