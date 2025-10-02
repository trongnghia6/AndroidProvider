package com.example.providerapp.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.data.model.auth.AuthDtos
import com.example.providerapp.data.repository.AuthRepository
import com.example.providerapp.data.repository.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class AuthViewModel : ViewModel() {

    private val notificationService = NotificationService()
    private val authRepository = AuthRepository()

    var isLoading by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
    var isSignUpSuccess by mutableStateOf<Boolean?>(null)
        private set

    suspend fun countUsersByEmail(email: String): Int = authRepository.countProviderUsersByEmail(email)

    fun signIn(email: String, password: String, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = authRepository.signInProvider(email, password)
                if (user != null){
                    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    val userId = user.id
                    val userName = user.name
                    sharedPref.edit {
                        putString("user_id", userId)
                        putString("username", userName)
                        apply()
                    }


                    // Gửi thông báo đăng nhập thành công và generate FCM token
                    launch {
                        try {
                            val success = notificationService.sendLoginSuccessNotification(userId, userName)
                            if (success) {
                                Log.d("AuthViewModel", "Login success notification sent to user: $userName")
                            } else {
                                Log.w("AuthViewModel", "Failed to send login success notification")
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error sending login notification: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        authRepository.uploadFcmToken(context, userId)
                    }

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }else{
                        Log.e("AuthViewModel", "Sign in failed - No user returned")
                    authError = "Không tìm thấy thông tin người dùng"
                    }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in error: ${e.message}", e)
                authError = "Đăng nhập thất bại: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        address: String,
        name: String,
        phoneNumber: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val role = "provider"
                val check = countUsersByEmail(email)
                if (check == 0){
                    val newUser = AuthDtos.UsersSignUp(
                        email = email,
                        password = password,
                        role = role,
                        address = address,
                        name = name,
                        phoneNumber = phoneNumber
                    )
                    authRepository.signUpProvider(newUser)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }else{
                    authError = "Email đã được đăng ký trước đó"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up error: ${e.message}", e)
                authError = "Đăng ký thất bại: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    fun clearError() {
        authError = null
    }

    /**
     * Đăng xuất người dùng
     * - Xóa tất cả SharedPreferences
     * - Logout khỏi Supabase Auth (nếu có session)
     * - Reset các state variables
     */
    fun logout(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isLoading = true

                // 1. Lấy thông tin user hiện tại để log
                val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val currentUserId = sharedPref.getString("user_id", "")
                val currentUserName = sharedPref.getString("username", "")

                Log.d("AuthViewModel", "Logging out user: $currentUserName (ID: $currentUserId)")

                // 2. Logout khỏi Supabase Auth (nếu có session)
                authRepository.signOutIfSessionExists()

                // 3. Xóa tất cả SharedPreferences
                try {
                    sharedPref.edit {
                        clear() // Xóa tất cả dữ liệu
                    }
                    Log.d("AuthViewModel", "Successfully cleared all SharedPreferences")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error clearing SharedPreferences: ${e.message}")
                    throw e
                }

                // 4. Reset các state variables
                withContext(Dispatchers.Main) {
                    authError = null
                    isSignUpSuccess = null
                    isLoading = false

                    Log.d("AuthViewModel", "Logout completed successfully")
                    onSuccess()
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Logout error: ${e.message}", e)
                authError = "Đăng xuất thất bại: ${e.message}"

                withContext(Dispatchers.Main) {
                    isLoading = false
                    // Vẫn gọi onSuccess() để navigate về login screen ngay cả khi có lỗi
                    // vì người dùng đã muốn đăng xuất
                    onSuccess()
                }
            }
        }
    }
}