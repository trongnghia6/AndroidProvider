//package com.example.testappcc.presentation.home
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowForward
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.testappcc.data.model.Task
//import java.time.LocalDate
//import java.time.format.DateTimeFormatter
//
//@Composable
//fun TaskListByDate(
//    date: LocalDate,
//    tasks: List<Task>,
//    onDateClick: () -> Unit
//) {
//    var selectedTask by remember { mutableStateOf<Task?>(null) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//            .background(Color.White, RoundedCornerShape(12.dp))
//            .padding(16.dp)
//    ) {
//        // Date header with clickable functionality
//        Text(
//            text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
//            fontSize = 20.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = Color(0xFF1E88E5),
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onDateClick() }
//                .padding(bottom = 12.dp)
//        )
//
//        if (tasks.isEmpty()) {
//            Text(
//                text = "Không có công việc",
//                fontSize = 16.sp,
//                color = Color.Gray,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//        } else {
//            LazyColumn(
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(tasks) { task ->
//                    TaskItem(task) { selectedTask = task }
//                }
//            }
//        }
//    }
//
//    // Task detail dialog
//    selectedTask?.let { task ->
//        AlertDialog(
//            onDismissRequest = { selectedTask = null },
//            title = {
//                Text(
//                    text = task.nameService,
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            },
//            text = {
//                Column {
//                    Text(
//                        text = "Ngày: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
//                        fontSize = 16.sp
//                    )
//                    task.startTime?.let {
//                        Text(
//                            text = "Giờ bắt đầu: $it",
//                            fontSize = 16.sp
//                        )
//                    }
//                    task.endTime?.let {
//                        Text(
//                            text = "Giờ kết thúc: $it",
//                            fontSize = 16.sp
//                        )
//                    }
//                    task.description?.let {
//                        Text(
//                            text = "Mô tả: $it",
//                            fontSize = 16.sp
//                        )
//                    }
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = { selectedTask = null }) {
//                    Text("Đóng")
//                }
//            },
//            containerColor = Color.White,
//            shape = RoundedCornerShape(12.dp)
//        )
//    }
//}
//
//@Composable
//fun TaskItem(task: Task, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() },
//        shape = RoundedCornerShape(8.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color(0xFFF5F7FA)
//        ),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = task.nameService,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = Color.Black
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Row {
//                    task.startTime?.let {
//                        Text(
//                            text = "Bắt đầu: $it",
//                            fontSize = 14.sp,
//                            color = Color(0xFF1976D2)
//                        )
//                    }
//                    task.endTime?.let {
//                        Text(
//                            text = " - Kết thúc: $it",
//                            fontSize = 14.sp,
//                            color = Color(0xFF1976D2),
//                            modifier = Modifier.padding(start = 8.dp)
//                        )
//                    }
//                }
//            }
//            Icon(
//                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
//                contentDescription = "View details",
//                tint = Color(0xFF1E88E5)
//            )
//        }
//    }
//}
//
//// Assuming Task model has been updated to include endTime and description
//data class Task(
//    val nameService: String,
//    val startTime: String?,
//    val endTime: String?,
//    val description: String?
//)