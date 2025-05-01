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
        val client = OkHttpClient.Builder()
            .apply {
                token?.let { t ->
                    addInterceptor(Interceptor { chain ->
                        val request = chain.request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer $t")
                            .build()
                        chain.proceed(request)
                    })
                }
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://mobile-liard.vercel.app/api/")
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
}
