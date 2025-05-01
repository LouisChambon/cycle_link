package com.example.cycle_link.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val address: String?
)
