package com.example.cycle_link.model

import retrofit2.http.GET
import retrofit2.http.Path

interface BikeApiService {
    @GET("bikes")
    suspend fun getBikes(): List<BikeAd>
    
    @GET("bikes/{id}")
    suspend fun getBikeById(@Path("id") id: String): BikeAd
} 