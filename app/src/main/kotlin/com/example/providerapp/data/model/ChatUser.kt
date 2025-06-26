package com.example.providerapp.data.model

data class ChatUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val role: String
) 