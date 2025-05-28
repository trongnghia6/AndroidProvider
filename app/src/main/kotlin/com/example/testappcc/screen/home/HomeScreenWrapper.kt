package com.example.testappcc.screen.home

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.testappcc.core.network.RetrofitClient
import com.example.testappcc.presentation.search.MapboxSuggestionScreen
import com.example.testappcc.presentation.BottomNavItem
import com.example.testappcc.model.viewmodel.TaskCalendarScreen
import com.example.testappcc.data.model.TaskViewModel
import com.example.testappcc.presentation.UserProfileScreen
import com.example.testappcc.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreenWrapper(onLogout: () -> Unit) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Trang chủ", "home_main", Icons.Default.Home),
        BottomNavItem("Tìm kiếm", "search_main", Icons.Default.Search),
        BottomNavItem("Lịch", "calendar_main", Icons.Default.DateRange),
        BottomNavItem("Tài khoản", "profile_main", Icons.Default.Person),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.name) },
                        label = { Text(item.name) }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home_main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home_main") {
                // Khởi tạo viewModel bằng viewModel() để lifecycle được quản lý đúng
                val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                val isLoading = homeViewModel.isLoading.value

                if (isLoading) {
                    Text("Đang tải...")
                } else {
                    HomeScreen(
                        onLogout = onLogout,
                        viewModel = homeViewModel
                    )
                }
            }

            composable("search_main") {
                MapboxSuggestionScreen(RetrofitClient.mapboxGeocodingService)
            }

            composable("profile_main") {
                UserProfileScreen(onLogout = onLogout)
            }

            composable("calendar_main") {
                val context = LocalContext.current
                val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val userId = sharedPref.getString("user_id", null) ?: "unknown_user"

                val taskViewModel = remember { TaskViewModel(userId) }
                TaskCalendarScreen(viewModel = taskViewModel)
            }
        }
    }
}

