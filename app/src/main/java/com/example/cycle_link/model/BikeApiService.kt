package com.example.cycle_link.model

import retrofit2.Response
import retrofit2.http.*

data class BikeRequest(
    val title      : String,
    val description: String,
    val price      : Double,
    val category   : String,
    val condition  : String,
    val imageUrl   : String?,
    val latitude   : Double?,
    val longitude  : Double?
)

interface BikeApiService {
    @GET("bikes")
    suspend fun getBikes(): List<BikeAd>

    @GET("bikes/{id}")
    suspend fun getBikeById(@Path("id") id: String): BikeAd

    @POST("bikes")
    suspend fun createBike(@Body bike: BikeRequest): BikeAd

    @PUT("bikes/{id}")
    suspend fun updateBike(
        @Path("id") id: String,
        @Body bike: BikeRequest
    ): BikeAd

    @DELETE("bikes/{id}")
    suspend fun deleteBike(@Path("id") id: String): Response<Unit>
}
