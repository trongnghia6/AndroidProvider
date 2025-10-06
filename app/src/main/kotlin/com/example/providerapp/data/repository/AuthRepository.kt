package com.example.providerapp.data.repository

import android.content.Context
import com.example.providerapp.core.MyFirebaseMessagingService
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.auth.AuthDtos
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class AuthRepository {

    suspend fun countProviderUsersByEmail(email: String): Int {
        return try {
            val result = supabase.from("users")
                .select(columns = Columns.list("email", "password", "role")) {
                    filter {
                        eq("email", email)
                        eq("role", "provider")
                    }
                }
                .decodeList<AuthDtos.UsersSignUp>()
            if (result.isEmpty()) 0 else 1
        } catch (_: Exception) {
            0
        }
    }

    suspend fun signInProvider(email: String, password: String): AuthDtos.UserSignIn? {
        return try {
            val users = supabase.from("users")
                .select(columns = Columns.list("email", "password", "id", "name", "lock")) {
                    filter {
                        eq("email", email)
                        eq("password", password)
                        eq("role", "provider")
                    }
                }
                .decodeList<AuthDtos.UserSignIn>()
            users.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signUpProvider(user: AuthDtos.UsersSignUp): Boolean {
        return try {
            supabase.from("users").insert(user)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signOutIfSessionExists() {
        try {
            val currentSession = supabase.auth.currentSessionOrNull()
            if (currentSession != null) {
                supabase.auth.signOut()
            }
        } catch (_: Exception) {
        }
    }

    fun uploadFcmToken(context: Context, userId: String) {
        MyFirebaseMessagingService.generateAndUploadToken(context, userId)
        MyFirebaseMessagingService.uploadPendingToken(context, userId)
    }
}