package com.example.testappcc.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testappcc.data.model.Bookings
import com.example.testappcc.model.viewmodel.ProviderHomeViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ProviderHomeScreen(
    viewModel: ProviderHomeViewModel = viewModel(),
    reminder: String? = null
) {
    val bookings = viewModel.bookings
    val errorMessage = viewModel.errorMessage

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val userName = sharedPref.getString("username", "User")
    val providerId = sharedPref.getString("user_id", "User")?: "User"

    LaunchedEffect(providerId) {
        viewModel.loadBookings(providerId)
        viewModel.loadPendingTasks(providerId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Xin chào, $userName 👋", style = MaterialTheme.typography.headlineSmall)
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi"))),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Lịch hẹn hôm nay:", style = MaterialTheme.typography.titleMedium)
        when {
            errorMessage != null -> {
                Text("Lỗi tải dữ liệu: $errorMessage", color = Color.Red)
            }
            bookings.isEmpty() -> {
                Text("Bạn chưa có lịch hẹn nào hôm nay.")
            }
            else -> {
                BookingList(bookings)
            }
        }

        Text("Công việc đang chờ xử lý:", style = MaterialTheme.typography.titleMedium)

        if (viewModel.pendingTasks.isEmpty()) {
            Text("Không có công việc nào đang chờ.")
        } else {
            LazyColumn {
                items(viewModel.pendingTasks) { task ->
                    PendingTaskCard(
                        task = task,
                        onAccept = { taskId -> viewModel.acceptTask(taskId, providerId) },
                        onReject = { taskId -> viewModel.rejectTask(taskId, providerId) }
                    )
                }
            }
        }


        reminder?.let {
            Spacer(modifier = Modifier.height(16.dp))
            ReminderCard(it)
        }
    }
}

@Composable
fun BookingList(bookings: List<Bookings>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(bookings) { booking ->
            AppointmentCard(appointment = booking)
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Bookings,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$startTime - $endTime - ${appointment.nameServices}")
            Text(
                "Khách hàng: ${appointment.customerName}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
fun ReminderCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFF856404))
            Spacer(Modifier.width(8.dp))
            Text(message, color = Color(0xFF856404))
        }
    }
}

@Composable
fun PendingTaskCard(
    task: Bookings,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${task.startAt} - ${task.nameServices}", style = MaterialTheme.typography.bodyLarge)
            Text("Khách hàng: ${task.customerName}", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onReject(task.id) }) {
                    Text("Từ chối", color = Color.Red)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onAccept(task.id) }) {
                    Text("Chấp nhận", color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

