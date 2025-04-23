package com.example.cycle_link.model

data class BikeAd(
    val id: String,
    val title: String,
    val sellerName: String,
    val imageUrl: String,
    val description: String,
    val price: Double,
    val contactEmail: String,
    val contactPhone: String
) 