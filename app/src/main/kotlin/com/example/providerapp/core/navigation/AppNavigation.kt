package com.example.providerapp.core.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.providerapp.core.network.MapboxGeocodingService
import com.example.providerapp.core.network.RetrofitClient
import com.example.providerapp.model.viewmodel.AuthViewModel
import com.example.providerapp.core.supabase
import com.example.providerapp.presentation.auth.LoginScreen
import com.example.providerapp.presentation.search.MapboxSuggestionScreen
import com.example.providerapp.presentation.auth.RegisterScreen
import com.example.providerapp.presentation.notification.NotificationScreen
import com.example.providerapp.screen.home.HomeScreenWrapper
import com.example.providerapp.presentation.search.SearchUsersScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun AppNavigation( initialRoute: String? = null) {
    val navController = rememberNavController()
//    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                // Nếu có initialRoute từ notification, navigate đến đó
                if (initialRoute == "notifications") {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                    // Delay một chút rồi navigate đến notifications
                    kotlinx.coroutines.delay(500)
                    navController.navigate("notifications")
                } else {
                    navController.navigate("search") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGotoRegister = {
                    navController.navigate("register") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }
        composable("register") {
            val authViewModel : AuthViewModel = viewModel()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val geocodingService = retrofit.create(MapboxGeocodingService::class.java)
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                goBack = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                viewModel = authViewModel,
                geocodingService = geocodingService
            )
        }
        composable("search") {
            MapboxSuggestionScreen(RetrofitClient.mapboxGeocodingService)
        }
        composable("home") {
            HomeScreenWrapper(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("search_users") {
            SearchUsersScreen(
                onBackClick = { navController.popBackStack() },
                onUserClick = { user ->
                    // Create new chat or navigate to existing chat
                    // For now, navigate to a new chat
                    navController.navigate("chat/new_${user.id}/${user.id}/${user.name}")
                }
            )
        }

        composable("notifications") {
            NotificationScreen(
                navController = navController
            )
        }
    }
}