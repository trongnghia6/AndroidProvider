package com.example.testappcc.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.testappcc.data.model.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarView(tasksByDate: Map<LocalDate, List<Task>>) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Monthly Calendar
        MonthlyCalendar(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            tasksByDate = tasksByDate,
            onDateSelected = { selectedDate = it },
            onMonthChange = { currentMonth = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Task List for Selected Date
        TaskListByDate(
            date = selectedDate,
            tasks = tasksByDate[selectedDate].orEmpty(),
            onDateClick = { showDatePicker = true }
        )

        // DatePicker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = { newDate ->
                    selectedDate = newDate
                    currentMonth = YearMonth.from(newDate)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Text("Trước")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Text("Sau")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                Text(
                    text = day,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Calendar grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ...
        val daysInMonth = lastDayOfMonth.dayOfMonth
        val totalSlots = (daysInMonth + firstDayOfWeek + 6) / 7 * 7 // Ensure full weeks

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items((0 until totalSlots step 7).toList()) { weekStart ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until 7) {
                        val dayOffset = weekStart + i - firstDayOfWeek
                        val date = if (dayOffset >= 0 && dayOffset < daysInMonth) {
                            firstDayOfMonth.plusDays(dayOffset.toLong())
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    if (date == selectedDate) Color(0xFF1E88E5) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = date != null) {
                                    date?.let { onDateSelected(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clickable { onDateSelected(date) }
                                ) {
                                    // Background for selected date
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (date == selectedDate) Color(0xFF1E88E5) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            color = if (date == selectedDate) Color.White else Color.Black,
                                            fontSize = 14.sp,
                                            fontWeight = if (tasksByDate[date]?.isNotEmpty() == true) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Badge for task count
                                    val taskCount = tasksByDate[date]?.size ?: 0
                                    if (taskCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .background(Color.Red, shape = RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = taskCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListByDate(
    date: LocalDate,
    tasks: List<Task>,
    onDateClick: () -> Unit
) {
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Date header with clickable functionality
        Text(
            text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E88E5),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDateClick() }
                .padding(bottom = 12.dp)
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Không có công việc",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(task) { selectedTask = task }
                }
            }
        }
    }

    // Task detail dialog
    selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTask = null },
            title = {
                Text(
                    text = task.nameService,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    task.nameCustomer?.let {
                        Text(
                            text = "Tên khách hàng: $it",
                            fontSize = 16.sp
                        )
                    }
                    task.phoneNumber?.let {
                        Text(
                            text = "Số điện thoại: $it",
                            fontSize = 16.sp
                        )
                    }
                    task.location?.let {
                        Text(
                            text = "Địa chỉ: $it",
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Ngày: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        fontSize = 16.sp
                    )
                    task.startTime?.let {
                        Text(
                            text = "Giờ bắt đầu: $it",
                            fontSize = 16.sp
                        )
                    }
                    task.endTime?.let {
                        Text(
                            text = "Giờ kết thúc: $it",
                            fontSize = 16.sp
                        )
                    }
                    task.description?.let {
                        Text(
                            text = "Mô tả: $it",
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTask = null }) {
                    Text("Đóng")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun TaskItem(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7FA)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = task.nameService,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    task.startTime?.let {
                        Text(
                            text = "Bắt đầu: $it",
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                    task.endTime?.let {
                        Text(
                            text = " - Kết thúc: $it",
                            fontSize = 14.sp,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View details",
                tint = Color(0xFF1E88E5)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chọn ngày",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Material3 DatePicker
                val datePickerState = rememberDatePickerState()
                DatePicker(state = datePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selected = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateSelected(selected)
                            }
                            onDismiss()
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
