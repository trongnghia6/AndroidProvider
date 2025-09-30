package com.example.providerapp.ui.tasks

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun TaskCalendarScreen(viewModel: TaskViewModel) {

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null) ?: "unknown_user"

    LaunchedEffect(Unit) {
        viewModel.fetchTasks(userId)
    }
    val taskList by viewModel.tasks.collectAsState()

    // Nhóm công việc theo ngày
    val tasksByDate = remember(taskList) {
        taskList
            .filter { it.status == "accepted" }  // Lọc theo trạng thái confirmed
            .groupBy { it.startDate }
    }

    CalendarView(tasksByDate)
}
