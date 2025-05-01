package com.example.cycle_link.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BikeRepository(private val token: String?) {
    private val apiService: BikeApiService

    init {
        val client = OkHttpClient.Builder().apply {
            token?.let {
                addInterceptor(Interceptor { chain ->
                    val newReq = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(newReq)
                })
            }
        }.build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://back-cyclelink.vercel.app/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(BikeApiService::class.java)
    }

    suspend fun getBikes(): List<BikeAd> = withContext(Dispatchers.IO) {
        runCatching { apiService.getBikes() }
            .getOrNull()
            .orEmpty()
    }

    suspend fun getBikeById(id: String): BikeAd? = withContext(Dispatchers.IO) {
        runCatching { apiService.getBikeById(id) }
            .getOrNull()
    }

    suspend fun createBike(request: BikeRequest): BikeAd? = withContext(Dispatchers.IO) {
        runCatching { apiService.createBike(request) }
            .getOrNull()
    }

    suspend fun updateBike(id: String, request: BikeRequest): BikeAd? =
        withContext(Dispatchers.IO) {
            runCatching { apiService.updateBike(id, request) }
                .getOrNull()
        }

    suspend fun deleteBike(id: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resp = apiService.deleteBike(id)
            resp.isSuccessful
        }.getOrDefault(false)
    }
}
