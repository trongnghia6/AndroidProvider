package com.example.providerapp.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.data.model.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement actual API call to load chats
                // For now, using mock data
                val mockChats = generateMockChats()
                _chats.value = mockChats
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchChats(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                loadChats()
            } else {
                val filteredChats = _chats.value.filter { chat ->
                    chat.otherUserName.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)
                }
                _chats.value = filteredChats
            }
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            val updatedChats = _chats.value.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(unreadCount = 0)
                } else chat
            }
            _chats.value = updatedChats
        }
    }

    private fun generateMockChats(): List<Chat> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            Chat(
                id = "1",
                userId = "user1",
                providerId = "provider1",
                lastMessage = "Xin chào, tôi cần dịch vụ sửa chữa",
                lastMessageTime = currentTime - 3600000, // 1 hour ago
                unreadCount = 2,
                isActive = true,
                createdAt = currentTime - 86400000, // 1 day ago
                otherUserName = "Nguyễn Văn A",
                otherUserAvatar = null
            ),
            Chat(
                id = "2",
                userId = "user2",
                providerId = "provider1",
                lastMessage = "Cảm ơn bạn đã hỗ trợ",
                lastMessageTime = currentTime - 7200000, // 2 hours ago
                unreadCount = 0,
                isActive = true,
                createdAt = currentTime - 172800000, // 2 days ago
                otherUserName = "Trần Thị B",
                otherUserAvatar = null
            ),
            Chat(
                id = "3",
                userId = "user3",
                providerId = "provider1",
                lastMessage = "Khi nào bạn có thể đến?",
                lastMessageTime = currentTime - 10800000, // 3 hours ago
                unreadCount = 1,
                isActive = true,
                createdAt = currentTime - 259200000, // 3 days ago
                otherUserName = "Lê Văn C",
                otherUserAvatar = null
            )
        )
    }
} 