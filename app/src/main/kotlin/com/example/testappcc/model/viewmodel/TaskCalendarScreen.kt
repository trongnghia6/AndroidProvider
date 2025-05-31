package com.example.testappcc.model.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.example.testappcc.presentation.home.CalendarView
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.testappcc.data.model.TaskViewModel

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
            .filter { it.status == "confirmed" }  // Lọc theo trạng thái confirmed
            .groupBy { it.startDate }
    }

    CalendarView(tasksByDate)
}
