package com.example.cycle_link.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BikeRepository {
    private val apiService: BikeApiService
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://yourapi.com/api/") // Replace with your actual API URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        apiService = retrofit.create(BikeApiService::class.java)
    }
    
    suspend fun getBikes(): List<BikeAd> = withContext(Dispatchers.IO) {
        return@withContext try {
            apiService.getBikes()
        } catch (e: Exception) {
            // In a real app, handle errors properly
            emptyList()
        }
    }
    
    suspend fun getBikeById(id: String): BikeAd? = withContext(Dispatchers.IO) {
        return@withContext try {
            apiService.getBikeById(id)
        } catch (e: Exception) {
            // In a real app, handle errors properly
            null
        }
    }
} 