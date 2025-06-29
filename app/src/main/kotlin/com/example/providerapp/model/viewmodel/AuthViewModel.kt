package com.example.providerapp.model.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.providerapp.core.supabase
import android.util.Log
import com.example.providerapp.core.MyFirebaseMessagingService
import com.example.providerapp.data.repository.NotificationService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsersSignUp(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("phone_number")
    val phoneNumber: String,
    val role: String,
    val address: String
)
@Serializable
data class UserSignIn(
    val id : String,
    val email: String,
    val name: String,
    val password: String
)

class AuthViewModel : ViewModel() {

    private val notificationService = NotificationService()

    var isLoading by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
    var isSignUpSuccess by mutableStateOf<Boolean?>(null)
        private set

    suspend fun countUsersByEmail(email: String): Int {
        return try {
            val count = supabase.from("users")
                .select(columns = Columns.list("email", "password", "role")) {
                    filter {
                        eq("email", email)
                        eq("role", "provider")
                    }
                }
                .decodeList<UsersSignUp>()
            if (count.isEmpty()){
                0
            }else{
                1
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error counting users: ${e.message}")
            0
        }
    }

    fun signIn(email: String, password: String, context: Context,onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val check =  supabase.from("users")
                        .select(columns = Columns.list("email", "password", "id", "name")) {
                            filter {
                                eq("email", email)
                                eq("password", password)
                                eq("role", "provider")
                            }
                        }
                    .decodeList<UserSignIn>()
                if (check.isNotEmpty()){
                    val user = check.first() // Lấy người dùng đầu tiên trong danh sách
                    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    val userId = user.id
                    val userName = user.name
                    with(sharedPref.edit()) {
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

                    // Generate and upload FCM token for push notifications
                    withContext(Dispatchers.Main) {
                        MyFirebaseMessagingService.generateAndUploadToken(context, userId)
                        MyFirebaseMessagingService.uploadPendingToken(context, userId)
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
                    val newUser = UsersSignUp(email = email, password = password, role = role, address = address, name = name, phoneNumber = phoneNumber  )
                    supabase.from("users").insert(newUser)
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
                try {
                    val currentSession = supabase.auth.currentSessionOrNull()
                    if (currentSession != null) {
                        supabase.auth.signOut()
                        Log.d("AuthViewModel", "Successfully signed out from Supabase Auth")
                    } else {
                        Log.d("AuthViewModel", "No active Supabase Auth session found")
                    }
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Error during Supabase Auth signout: ${e.message}")
                    // Tiếp tục với logout process ngay cả khi Supabase signout failed
                }

                // 3. Xóa tất cả SharedPreferences
                try {
                    with(sharedPref.edit()) {
                        clear() // Xóa tất cả dữ liệu
                        apply()
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

    /**
     * Đăng xuất nhanh - chỉ xóa SharedPreferences và navigate
     * Sử dụng khi cần logout ngay lập tức mà không cần xử lý async
     */
    fun quickLogout(context: Context, onSuccess: () -> Unit) {
        try {
            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val currentUserName = sharedPref.getString("username", "")
            
            Log.d("AuthViewModel", "Quick logout for user: $currentUserName")
            
            // Xóa tất cả SharedPreferences
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            
            // Reset state variables
            authError = null
            isSignUpSuccess = null
            isLoading = false
            
            Log.d("AuthViewModel", "Quick logout completed")
            onSuccess()
            
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Quick logout error: ${e.message}", e)
            authError = "Đăng xuất thất bại: ${e.message}"
            // Vẫn gọi onSuccess() để đảm bảo user có thể logout
            onSuccess()
        }
    }
}
