package com.example.providerapp.data.model

data class Chat(
    val id: String,
    val userId: String,
    val providerId: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val otherUserName: String = "",
    val otherUserAvatar: String? = null
) 