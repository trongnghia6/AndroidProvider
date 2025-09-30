package com.example.providerapp.ui.home

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.providerapp.core.network.RetrofitClient
import com.example.providerapp.data.model.BottomNavItem
import com.example.providerapp.ui.tasks.TaskCalendarScreen
import com.example.providerapp.ui.services.ServiceManagementScreen
import com.example.providerapp.ui.profile.UserProfileView
import com.example.providerapp.ui.chat.ChatListScreen
import com.example.providerapp.ui.chat.ChatScreen
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.providerapp.ui.profile.AvatarChangeScreen
import com.example.providerapp.ui.services.ServiceDetailsScreen
import com.example.providerapp.data.model.ServiceWithDetails
import com.example.providerapp.ui.notifications.NotificationScreen
import com.example.providerapp.ui.suggestion.MapboxSuggestionScreen
import com.example.providerapp.ui.suggestion.SearchUsersScreen
import com.example.providerapp.ui.tasks.TaskViewModel
import kotlinx.serialization.json.Json

@Composable
fun HomeScreenWrapper(
    onLogout: () -> Unit,
    navController: NavController? = null
) {
    val internalNavController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Trang chủ", "home_main", Icons.Default.Home),
        BottomNavItem("Dịch vụ", "service_management", Icons.Default.HomeRepairService),
        BottomNavItem("Tin nhắn", "chat_main", Icons.AutoMirrored.Filled.Message),
        BottomNavItem("Lịch", "calendar_main", Icons.Default.DateRange),
        BottomNavItem("Hồ sơ", "profile_main", Icons.Default.Person),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = internalNavController.currentBackStackEntryAsState().value?.destination?.route
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            internalNavController.navigate(item.route) {
                                popUpTo(internalNavController.graph.startDestinationId) { saveState = true }
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
            navController = internalNavController,
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

                    ProviderHomeScreen(
                        navController = internalNavController
                    )
                }
            }

            composable("search_main") {
                MapboxSuggestionScreen(RetrofitClient.mapboxGeocodingService)
            }

            composable("chat_main") {
                ChatListScreen(navController = internalNavController)
            }

            composable("profile_main") {
                UserProfileView(
                    onLogout = onLogout, // Truyền onLogout callback
                    onAvatarClick = {
                        internalNavController.navigate("avatar_change")
                    }
                )
            }

            composable("calendar_main") {
                val context = LocalContext.current
                val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val userId = sharedPref.getString("user_id", null) ?: "unknown_user"

                val taskViewModel = remember { TaskViewModel() }
                TaskCalendarScreen(viewModel = taskViewModel)
            }
            // Trong NavHost hoặc Navigation setup
            composable("service_management") {
                ServiceManagementScreen(
                    onBackClick = { internalNavController.popBackStack() },
                    onServiceClick = { service ->
                        try {
                            val serviceJson = Json.encodeToString(service)
                            internalNavController.navigate("service_details/$serviceJson")
                        } catch (e: Exception) {
                            // Handle encoding error
                        }
                    }
                )
            }

            composable(
                "service_details/{serviceData}",
                arguments = listOf(navArgument("serviceData") { type = NavType.StringType })
            ) { backStackEntry ->

                val serviceDataJson = backStackEntry.arguments?.getString("serviceData")

                // Decode trước khi render composable
                val service = remember(serviceDataJson) {
                    try {
                        serviceDataJson?.let { Json.decodeFromString<ServiceWithDetails>(it) }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (service != null) {
                    ServiceDetailsScreen(
                        service = service,
                        onBackClick = { internalNavController.popBackStack() }
                    )
                } else {
                    // Nếu lỗi, có thể điều hướng hoặc hiển thị trạng thái lỗi
                    LaunchedEffect(Unit) {
                        internalNavController.popBackStack()
                    }
                }
            }

            // Chat routes
            composable(
                route = "chat/{providerId}",
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                ChatScreen(
                    providerId = backStackEntry.arguments?.getString("providerId") ?: "",
                    navController = internalNavController
                )
            }

            composable("search_users") {
                SearchUsersScreen(
                    onBackClick = { internalNavController.popBackStack() },
                    onUserClick = { user ->
                        // Create new chat or navigate to existing chat
                        internalNavController.navigate("chat_detail/new_${user.id}/${user.id}/${user.name}")
                    }
                )
            }

            composable("avatar_change") {
                AvatarChangeScreen(
                    onBackClick = { internalNavController.popBackStack() }
                )
            }
            composable("notifications") {
                NotificationScreen(
                    navController = internalNavController
                )
            }

        }
    }
}
