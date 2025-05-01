package com.example.cycle_link.model

import com.google.firebase.Timestamp

data class BikeAd(
    val id: String,
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val imageUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val seller: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null,
    val sellerName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = ""
)
