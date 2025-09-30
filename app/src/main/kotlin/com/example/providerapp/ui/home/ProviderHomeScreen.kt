package com.example.providerapp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.providerapp.BuildConfig
import com.example.providerapp.core.MyFirebaseMessagingService
import com.example.providerapp.data.model.Bookings
import com.example.providerapp.ui.notifications.NotificationViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.datetime.Instant as KxInstant


@Composable
fun ProviderHomeScreen(
    viewModel: ProviderHomeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    navController: NavController? = null,
    reminder: String? = null
) {
    val bookings = viewModel.bookings
    val errorMessage = viewModel.errorMessage
    var selectedTask by remember { mutableStateOf<Bookings?>(null) }

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userName = sharedPref.getString("username", "User")
    val providerId = sharedPref.getString("user_id", "User") ?: "User"
    var showReminder by remember { mutableStateOf(false) }

    // State để track notification count để detect thông báo mới
    var lastNotificationCount by remember { mutableIntStateOf(notificationViewModel.unreadCount) }

    // Function để refresh tất cả data
    val refreshData = {
        viewModel.loadBookings(providerId)
        viewModel.loadPendingTasks(providerId)
        notificationViewModel.loadNotifications(providerId)
    }

    // Load initial data
    LaunchedEffect(providerId) {
        refreshData()

        reminder?.let {
            viewModel.checkLocationDistance(
                context = context,
                reminderAddress = it,
                accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN,
                onResult = { isTooFar ->
                    showReminder = isTooFar
                }
            )
        }
    }

    // Watch notification count changes để refresh ngay khi có thông báo mới
    LaunchedEffect(notificationViewModel.unreadCount) {
        if (notificationViewModel.unreadCount > lastNotificationCount) {
            // Có thông báo mới, refresh data ngay lập tức
            refreshData()
        }
        lastNotificationCount = notificationViewModel.unreadCount
    }

    // Listen for FCM broadcast để refresh data khi có notification mới
    DisposableEffect(context) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MyFirebaseMessagingService.NEW_NOTIFICATION_ACTION) {
                    val notificationType = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE)
                    Log.d("ProviderHome", "Received notification broadcast, type: $notificationType")
                    
                    // Refresh data ngay lập tức
                    refreshData()
                }
            }
        }

        val intentFilter = IntentFilter(MyFirebaseMessagingService.NEW_NOTIFICATION_ACTION)
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            HeaderSection(userName = userName ?: "User")
        }

        // Notification Section
        item {
            NotificationSection(
                unreadCount = notificationViewModel.unreadCount,
                onNotificationClick = {
                    navController?.navigate("notifications")
                }
            )
        }

        // Reminder Card (if needed)
        if (showReminder) {
            item {
                ReminderCard("Vị trí hiện tại của bạn cách xa vị trí đã lưu. Vui lòng cập nhật.")
            }
        }

        // Today's Bookings Section
        item {
            val today = LocalDate.now()
            val todayBookings = bookings.filter { booking ->
                val systemZone = ZoneId.systemDefault()
                val localDate = OffsetDateTime.parse(booking.startAt.toString())
                    .atZoneSameInstant(systemZone)
                    .toLocalDate()

                localDate.isEqual(today)
            }

            TodayBookingsSection(
                bookings = todayBookings,
                errorMessage = errorMessage,
                onTaskClick = { selectedTask = it },
                onCompleteBooking = { bookingId, currentStatus ->
                    viewModel.completeBooking(bookingId, currentStatus, providerId)
                }
            )
        }

        // Pending Tasks Section
        item {
            PendingTasksSection(
                pendingTasks = viewModel.pendingTasks,
                onAccept = { taskId -> viewModel.acceptTask(taskId, providerId) },
                onReject = { taskId -> viewModel.rejectTask(taskId, providerId) },
                onTaskClick = { selectedTask = it }
            )
        }
    }

    // Task Detail Dialog
    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onAccept = { taskId ->
                viewModel.acceptTask(taskId, providerId)
                selectedTask = null
            },
            onReject = { taskId ->
                viewModel.rejectTask(taskId, providerId)
                selectedTask = null
            },
            isPendingTask = viewModel.pendingTasks.contains(task)
        )
    }
}

@Composable
private fun NotificationSection(
    unreadCount: Int,
    onNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Thông báo",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Thông báo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Xem thông báo",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TaskDetailDialog(
    task: Bookings,
    onDismiss: () -> Unit,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    isPendingTask: Boolean = false
) {
    val systemZone = ZoneId.systemDefault()
    val startDateTime = OffsetDateTime.parse(task.startAt.toString())
        .atZoneSameInstant(systemZone)
    val endDateTime = OffsetDateTime.parse(task.endAt.toString())
        .atZoneSameInstant(systemZone)

    val startTime = startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val endTime = endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = startDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chi tiết công việc",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service Info
                DetailInfoCard(
                    title = "Thông tin dịch vụ",
                    icon = Icons.Default.Settings
                ) {
                    DetailRow(
                        label = "Tên dịch vụ",
                        value = task.nameServices,
                        icon = Icons.Default.Star
                    )
                    DetailRow(
                        label = "Mô tả",
                        value = task.description ?: "Không có mô tả",
                        icon = Icons.Default.Info
                    )
                    DetailRow(
                        label = "Giá",
                        value = "${task.customPrice} VNĐ",
                        icon = Icons.Default.Star
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Info
                DetailInfoCard(
                    title = "Thời gian",
                    icon = Icons.Default.DateRange
                ) {
                    DetailRow(
                        label = "Ngày",
                        value = date,
                        icon = Icons.Default.DateRange
                    )
                    DetailRow(
                        label = "Giờ bắt đầu",
                        value = startTime,
                        icon = Icons.Default.Star
                    )
                    DetailRow(
                        label = "Giờ kết thúc",
                        value = endTime,
                        icon = Icons.Default.Star
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Info
                DetailInfoCard(
                    title = "Thông tin khách hàng",
                    icon = Icons.Default.Person
                ) {
                    DetailRow(
                        label = "Tên khách hàng",
                        value = task.customerName.toString(),
                        icon = Icons.Default.Person
                    )
                    DetailRow(
                        label = "Số điện thoại",
                        value = task.phoneNumber ?: "Không có",
                        icon = Icons.Default.Phone
                    )
                    DetailRow(
                        label = "Địa chỉ",
                        value = task.location,
                        icon = Icons.Default.LocationOn
                    )
                }

                // Action Buttons - only show for pending tasks
                if (isPendingTask) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onReject(task.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reject",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Từ chối")
                        }

                        Button(
                            onClick = { onAccept(task.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chấp nhận")
                        }
                    }
                } else {
                    // For confirmed bookings, show status
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Accepted",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lịch hẹn đã được xác nhận",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeaderSection(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xin chào, $userName 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi"))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun TodayBookingsSection(
    bookings: List<Bookings>,
    errorMessage: String?,
    onTaskClick: (Bookings) -> Unit,
    onCompleteBooking: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch hẹn hôm nay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (bookings.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = bookings.size.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                errorMessage != null -> {
                    ErrorMessage(errorMessage)
                }
                bookings.isEmpty() -> {
                    EmptyBookingsMessage()
                }
                else -> {
                    BookingList(bookings, onTaskClick, onCompleteBooking)
                }
            }
        }
    }
}

@Composable
private fun PendingTasksSection(
    pendingTasks: List<Bookings>,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onTaskClick: (Bookings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Tasks",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Công việc đang chờ xử lý",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (pendingTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = pendingTasks.size.toString(),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pendingTasks.isEmpty()) {
                EmptyTasksMessage()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingTasks.forEach { task ->
                        PendingTaskCard(
                            task = task,
                            onAccept = onAccept,
                            onReject = onReject,
                            onTaskClick = onTaskClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingList(
    bookings: List<Bookings>, 
    onTaskClick: (Bookings) -> Unit,
    onCompleteBooking: (Int, String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bookings.forEach { booking ->
            AppointmentCard(
                appointment = booking,
                onTaskClick = onTaskClick,
                onCompleteBooking = onCompleteBooking
            )
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Bookings,
    modifier: Modifier = Modifier,
    onTaskClick: (Bookings) -> Unit,
    onCompleteBooking: (Int, String) -> Unit
) {
    val systemZone = ZoneId.systemDefault()
    val startTime = OffsetDateTime.parse(appointment.startAt.toString())
        .atZoneSameInstant(systemZone)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    val endTime = OffsetDateTime.parse(appointment.endAt.toString())
        .atZoneSameInstant(systemZone)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main content row - clickable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTaskClick(appointment) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = startTime,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = endTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Service and customer info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.nameServices,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Customer",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = appointment.customerName.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status indicator
                Icon(
                    imageVector = when (appointment.status) {
                        "accepted" -> Icons.Default.CheckCircle
                        "c-confirmed" -> Icons.Default.Verified
                        "completed" -> Icons.Default.TaskAlt
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = "Status",
                    tint = when (appointment.status) {
                        "accepted" -> MaterialTheme.colorScheme.primary
                        "c-confirmed" -> MaterialTheme.colorScheme.secondary
                        "completed" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Complete button - only show for accepted and c-confirmed bookings
            if (appointment.status == "accepted" || appointment.status == "c-confirmed") {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { 
                            onCompleteBooking(appointment.id, appointment.status)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (appointment.status == "accepted") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (appointment.status == "accepted") 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.TaskAlt,
                            contentDescription = "Complete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appointment.status == "accepted") 
                                "Xác nhận" 
                            else 
                                "Hoàn thành",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Warning",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
@Composable
fun PendingTaskCard(
    task: Bookings,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onTaskClick: (Bookings) -> Unit
) {
    val formatterDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm")

    val startInstantKx: KxInstant = task.startAt // kotlinx.datetime.Instant
    val endInstantKx: KxInstant = task.endAt

    val userZone = ZoneId.systemDefault()
// ✅ Chuyển từng KxInstant sang java.time.Instant
    val startInUserZone = Instant.parse(startInstantKx.toString()).atZone(userZone)
    val endInUserZone = Instant.parse(endInstantKx.toString()).atZone(userZone)

    val date = startInUserZone.format(formatterDate)
    val startTime = startInUserZone.format(formatterTime)
    val endTime = endInUserZone.format(formatterTime)

    val text = "$date, $startTime - $endTime - ${task.nameServices}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Time",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Customer",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Khách hàng: ${task.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onReject(task.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Từ chối")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onAccept(task.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chấp nhận")
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(errorMessage: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Lỗi tải dữ liệu: $errorMessage",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun EmptyBookingsMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "No bookings",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bạn chưa có lịch hẹn nào hôm nay",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Hãy thư giãn và chuẩn bị cho ngày mai!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyTasksMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "No tasks",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Không có công việc nào đang chờ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tuyệt vời! Bạn đã xử lý hết công việc",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
