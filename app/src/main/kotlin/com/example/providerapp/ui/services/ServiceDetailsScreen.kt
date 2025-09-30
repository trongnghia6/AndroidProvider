package com.example.providerapp.ui.services

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Context
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    service: ServiceWithDetails,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    remember {
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_id", null)
    }

    var ratings by remember { mutableStateOf<List<ServiceRatingWithUser>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<ListBooking>>(emptyList()) }
    var isLoadingRatings by remember { mutableStateOf(true) }
    var isLoadingBookings by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var responseText by remember { mutableStateOf("") }
    var respondingToRatingId by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load ratings và bookings khi khởi tạo
    LaunchedEffect(service.id) {
        // Load ratings
        loadServiceRatings(service.id) { result, errorMsg ->
            ratings = result
            error = errorMsg
            isLoadingRatings = false
        }
        
        // Load bookings
        loadServiceBookings(service.id) { result, errorMsg ->
            bookings = result
            if (errorMsg != null && error == null) {
                error = errorMsg
            }
            isLoadingBookings = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Chi tiết dịch vụ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Information Card
            item {
                ServiceInfoCard(service = service)
            }

            // Bookings Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đơn hàng dịch vụ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${bookings.size} đơn hàng",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Loading state for bookings
            if (isLoadingBookings) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Empty state for bookings
            if (!isLoadingBookings && bookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có đơn hàng nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Bookings List
            items(bookings) { booking ->
                BookingCard(
                    booking = booking,
                    onBookingClick = { 
                        // TODO: Navigate to booking details
                        Log.d("ServiceDetails", "Clicked booking: ${booking.id}")
                    }
                )
            }

            // Ratings Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đánh giá dịch vụ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${ratings.size} đánh giá",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Loading state for ratings
            if (isLoadingRatings) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error state
            if (error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = "Lỗi: $error",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Empty state for ratings
            if (!isLoadingRatings && error == null && ratings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có đánh giá nào",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Ratings List
            items(ratings) { rating ->
                RatingCard(
                    rating = rating,
                    onRespondClick = { ratingId ->
                        respondingToRatingId = ratingId
                        responseText = ""
                    },
                    isResponding = respondingToRatingId == rating.id,
                    responseText = responseText,
                    onResponseTextChange = { responseText = it },
                    onSubmitResponse = {
                        coroutineScope.launch {
                            submitResponse(rating.id, responseText) { success, errorMsg ->
                                if (success) {
                                    respondingToRatingId = null
                                    responseText = ""
                                    // Reload ratings
                                    coroutineScope.launch {
                                        loadServiceRatings(service.id) { result, _ ->
                                            ratings = result
                                        }
                                    }
                                } else {
                                    error = errorMsg
                                }
                            }
                        }
                    },
                    onCancelResponse = {
                        respondingToRatingId = null
                        responseText = ""
                    }
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun ServiceInfoCard(service: ServiceWithDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${service.serviceTypeName} • ${service.specificServiceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    title = "Giá dịch vụ",
                    value = String.format("%,.0f₫", service.price),
                    valueColor = Color(0xFFE53935)
                )
                
                InfoItem(
                    title = "Số nhân viên",
                    value = "${service.numWorkers} người"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoItem(
                title = "Trạng thái",
                value = if (service.isActive) "Đang hoạt động" else "Đã tắt",
                valueColor = if (service.isActive) Color(0xFF4CAF50) else Color.Gray
            )
            
            if (!service.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Mô tả:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    title: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun BookingCard(
    booking: ListBooking,
    onBookingClick: (ListBooking) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = { onBookingClick(booking) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header với tên khách hàng và trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.users?.name.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: #${booking.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                BookingStatusChip(status = booking.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Thông tin thời gian
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BookingInfoItem(
                    icon = Icons.Default.Schedule,
                    title = "Bắt đầu",
                    value = formatTimestampToUserTimezonePretty(booking.startAt),
                    iconColor = Color(0xFF4CAF50)
                )
                
                BookingInfoItem(
                    icon = Icons.Default.TimerOff,
                    title = "Kết thúc",
                    value = formatTimestampToUserTimezonePretty(booking.endAt),
                    iconColor = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Thông tin liên hệ và địa chỉ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = booking.users?.phoneNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = booking.location.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Thông tin bổ sung nếu có
            Spacer(modifier = Modifier.height(12.dp))
                
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (booking.numberWorkers != null) {
                    BookingInfoItem(
                        icon = Icons.Default.Group,
                        title = "Nhân viên",
                        value = "${booking.numberWorkers} người",
                        iconColor = Color(0xFF607D8B)
                    )
                }

                BookingInfoItem(
                    icon = Icons.Default.AttachMoney,
                    title = "Giá",
                    value = String.format("%,.0f₫", booking.transactions.firstOrNull()?.amount),
                    iconColor = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
fun BookingInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    iconColor: Color = Color.Gray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BookingStatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "pending" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "accepted" -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        "c-confirmed" -> Color(0xFFE8F5E8) to Color(0xFF2E7D32)
        "p-confirmed" -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
        "completed" -> Color(0xFFE8F5E8) to Color(0xFF388E3C)
        "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        else -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }
    
    val statusText = when (status) {
        "pending" -> "Chờ xác nhận"
        "accepted" -> "Đã chấp nhận"
        "c-confirmed" -> "Khách xác nhận"
        "p-confirmed" -> "Nhà cung cấp xác nhận"
        "completed" -> "Hoàn thành"
        "cancelled" -> "Đã hủy"
        else -> status
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = statusText,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RatingCard(
    rating: ServiceRatingWithUser,
    onRespondClick: (Int) -> Unit,
    isResponding: Boolean,
    responseText: String,
    onResponseTextChange: (String) -> Unit,
    onSubmitResponse: () -> Unit,
    onCancelResponse: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // User info and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rating.users.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(rating.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            if (index < rating.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (index < rating.rating) Color(0xFFFFB300) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rating.rating.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Comment
            if (!rating.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Provider response
            if (!rating.responses.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Phản hồi từ nhà cung cấp",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rating.responses,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (!isResponding) {
                // Response button
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onRespondClick(rating.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trả lời đánh giá")
                }
            }
            
            // Response input
            if (isResponding) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = responseText,
                    onValueChange = onResponseTextChange,
                    label = { Text("Phản hồi của bạn") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelResponse,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = onSubmitResponse,
                        modifier = Modifier.weight(1f),
                        enabled = responseText.isNotBlank()
                    ) {
                        Text("Gửi phản hồi")
                    }
                }
            }
        }
    }
}

// Helper functions
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        Log.e("ServiceDetailsScreen", "Error format time: ${e.message}")
        dateString
    }
}

// Data loading functions
suspend fun loadServiceRatings(
    serviceId: Int,
    onResult: (List<ServiceRatingWithUser>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("service_ratings")
                .select(
                    columns = Columns.raw(
                        """
                        id, provider_service_id, user_id, rating, comment, responses, created_at, updated_at,
                        users!inner(
                            id, name, email, avatar
                        )
                        """.trimIndent()
                    )
                ) {
                    filter {
                        eq("provider_service_id", serviceId)
                    }
                    order(column = "created_at", order = Order.ASCENDING )
                }
                .decodeList<ServiceRatingWithUser>()

            withContext(Dispatchers.Main) {
                onResult(result, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList(), e.message)
            }
            Log.e("ServiceDetails", "Error loading service ratings", e)
        }
    }
}

suspend fun loadServiceBookings(
    providerServiceId: Int,
    onResult: (List<ListBooking>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("bookings")
                .select(
                    columns = Columns.raw(
                        """
                        id, customer_id, description, status, location, start_at, end_at, 
                        provider_service_id, number_workers,
                        transactions!inner(
                            id, amount, status, created_at
                        ),
                        provider_services!inner(
                            id, name, provider_id
                        ),
                        users!inner(
                            id, name, phone_number, avatar
                        )
                        """.trimIndent()
                    )
                ) {
                    filter {
                        and {
                            eq("provider_service_id", providerServiceId)
                        }
                    }
                    order(column = "start_at", order = Order.DESCENDING)
                }
                .decodeList<ListBooking>()

            withContext(Dispatchers.Main) {
                onResult(result, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList(), e.message)
            }
            Log.e("ServiceDetails", "Error loading service bookings", e)
        }
    }
}

suspend fun submitResponse(
    ratingId: Int,
    responses: String,
    onResult: (Boolean, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            supabase.from("service_ratings")
                .update(ServiceRatingUpdate(responses = responses)) {
                    filter {
                        eq("id", ratingId)
                    }
                }

            withContext(Dispatchers.Main) {
                onResult(true, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(false, e.message)
            }
            Log.e("ServiceDetails", "Error submitting response", e)
        }
    }
} 