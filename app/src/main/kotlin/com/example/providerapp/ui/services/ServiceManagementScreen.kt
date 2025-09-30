package com.example.providerapp.ui.services

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.ui.text.style.TextAlign
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceManagementScreen(
    onBackClick: () -> Unit = {},
    onServiceClick: (ServiceWithDetails) -> Unit // Thêm callback này
) {
    val context = LocalContext.current
    val providerId = remember {
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_id", null)
    }

    var services by remember { mutableStateOf<List<ServiceWithDetails>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showChangeStatusDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<ServiceWithDetails?>(null) }
    var serviceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var specificServices by remember { mutableStateOf<List<SpecificService>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Load dữ liệu khi khởi tạo
    LaunchedEffect(providerId) {
        if (providerId != null) {
            loadServices(providerId) { result, errorMsg ->
                services = result
                error = errorMsg
                isLoading = false
            }
            loadServiceTypes { types ->
                serviceTypes = types
            }
            loadSpecificServices { specific ->
                specificServices = specific
            }
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
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Quản lý dịch vụ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Thêm dịch vụ",
                        tint = Color.White
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lỗi: $error",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                error = null
                                if (providerId != null) {
                                    coroutineScope.launch {
                                        loadServices(providerId) { result, errorMsg ->
                                            services = result
                                            error = errorMsg
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            services.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.BusinessCenter,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có dịch vụ nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhấn nút + để thêm dịch vụ đầu tiên",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(services) { service ->
                        ServiceCard(
                            service = service,
                            onEditClick = {
                                selectedService = service
                                showEditDialog = true
                            },
                            onChangeStatusClick = {
                                selectedService = service
                                showChangeStatusDialog = true
                            },
                            onServiceClick = { onServiceClick(service) } // Truyền callback
                        )
                    }
                }
            }
        }
    }

    // Dialog thêm dịch vụ
    if (showAddDialog) {
        AddServiceDialog(
            serviceTypes = serviceTypes,
            specificServices = specificServices,
            onDismiss = { showAddDialog = false },
            onConfirm = { serviceInsert ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        supabase.from("provider_services").insert(serviceInsert)
                        withContext(Dispatchers.Main) {
                            showAddDialog = false
                            // Reload services
                            if (providerId != null) {
                                coroutineScope.launch {
                                    loadServices(providerId) { result, errorMsg ->
                                        services = result
                                        error = errorMsg
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            error = "Lỗi thêm dịch vụ: ${e.message}"
                        }
                        Log.e("ServiceManagement", "Error adding service", e)
                    }
                }
            }
        )
    }

    // Dialog sửa dịch vụ
    if (showEditDialog && selectedService != null) {
        EditServiceDialog(
            service = selectedService!!,
            serviceTypes = serviceTypes,
            specificServices = specificServices,
            onDismiss = {
                showEditDialog = false
                selectedService = null
            },
            onConfirm = { serviceUpdate ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        supabase.from("provider_services")
                            .update(serviceUpdate) {
                                filter {
                                    eq("id", selectedService!!.id)
                                }
                            }
                        withContext(Dispatchers.Main) {
                            showEditDialog = false
                            selectedService = null
                            // Reload services
                            if (providerId != null) {
                                coroutineScope.launch {
                                    loadServices(providerId) { result, errorMsg ->
                                        services = result
                                        error = errorMsg
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            error = "Lỗi cập nhật dịch vụ: ${e.message}"
                        }
                        Log.e("ServiceManagement", "Error updating service", e)
                    }
                }
            }
        )
    }

    // Dialog xác nhận thay đổi trạng thái
    if (showChangeStatusDialog && selectedService != null) {
        AlertDialog(
            onDismissRequest = {
                showChangeStatusDialog = false
                selectedService = null
            },
            title = { Text("Thay đổi trạng thái") },

            text = {
                if (selectedService!!.isActive) {
                    Text("Bạn có chắc chắn muốn tắt dịch vụ \"${selectedService!!.name}\"?")
                } else {
                    Text("Bạn có chắc chắn muốn bật dịch vụ \"${selectedService!!.name}\"?")
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val newStatus = !selectedService!!.isActive
                                supabase.from("provider_services")
                                    .update(mapOf("is_active" to newStatus)) {
                                        filter {
                                            eq("id", selectedService!!.id)
                                        }
                                    }
                                withContext(Dispatchers.Main) {
                                    showChangeStatusDialog = false
                                    selectedService = null
                                    if (providerId != null) {
                                        coroutineScope.launch {
                                            loadServices(providerId) { result, errorMsg ->
                                                services = result
                                                error = errorMsg
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    error = "Lỗi thay đổi trạng thái: ${e.message}"
                                }
                                Log.e("ServiceManagement", "Error changing status", e)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedService!!.isActive) Color.Red else Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        if (selectedService!!.isActive) "Tắt" else "Bật",
                        color = Color.White
                    )
                }
            },

            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showChangeStatusDialog = false
                        selectedService = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun ServiceCard(
    service: ServiceWithDetails,
    onEditClick: () -> Unit,
    onChangeStatusClick: () -> Unit,
    onServiceClick: () -> Unit // Thêm callback này
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable { onServiceClick() }, // Sửa lại để gọi onServiceClick
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${service.serviceTypeName} • ${service.specificServiceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (service.isActive) "Đang hoạt động" else "Đã tắt",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (service.isActive) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Sửa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onChangeStatusClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ChangeCircle,
                            contentDescription = "Xóa",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Giá",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("%,.0f₫", service.price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Số nhân viên",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${service.numWorkers} người",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!service.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Hàm load dữ liệu
suspend fun loadServices(
    providerId: String,
    onResult: (List<ServiceWithDetails>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("provider_services")
                .select(
                    columns = Columns.raw(
                        """
                        id, name, number_staff,
                        service_id, provider_id, custom_price, custom_description, is_active,
                        services!inner(
                            name, service_type_id,
                            service_types!inner(name)
                        )
                        """.trimIndent()
                    )
                ) {
                    filter {
                        eq("provider_id", providerId)
                    }
                }
                .decodeList<ServiceWithDetails>()

            withContext(Dispatchers.Main) {
                onResult(result, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList(), e.message)
            }
            Log.e("ServiceManagement", "Error loading services", e)
        }
    }
}


suspend fun loadServiceTypes(onResult: (List<ServiceType>) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("service_types")
                .select()
                .decodeList<ServiceType>()

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
            Log.e("ServiceManagement", "Error loading service types", e)
        }
    }
}

suspend fun loadSpecificServices(onResult: (List<SpecificService>) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("services")
                .select()
                .decodeList<SpecificService>()

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
            Log.e("ServiceManagement", "Error loading specific services", e)
        }
    }
}
