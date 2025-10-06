package com.example.providerapp.data.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AuthDtos {
    @Serializable
    data class UsersSignUp(
        val name: String,
        val email: String,
        val password: String,
        @SerialName("phone_number")
        val phoneNumber: String,
        val role: String,
        val address: String,
        @SerialName("paypal_email")
        val paypalEmail: String
    )
    @Serializable
    data class UserSignIn(
        val id : String,
        val email: String,
        val name: String,
        val password: String,
        val lock: String
    )
}