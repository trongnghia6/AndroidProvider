package com.example.providerapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Users(
    var id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val address: String,
    @SerialName("phone_number")
    val phoneNumber: String,
    val avatar: String ?= null,

)
