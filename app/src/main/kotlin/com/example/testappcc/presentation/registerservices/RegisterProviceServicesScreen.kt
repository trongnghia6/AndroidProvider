package com.example.testappcc.presentation.registerservices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testappcc.data.model.ProviderServicesInsert
import com.example.testappcc.data.model.Service
import com.example.testappcc.data.model.ServiceType
import com.example.testappcc.model.viewmodel.RegisterServiceViewModel

@Composable
fun RegisterServiceScreen(viewModel: RegisterServiceViewModel = viewModel()) {
    val serviceTypes = viewModel.serviceTypes
    val services = viewModel.services

    var selectedType by remember { mutableStateOf<ServiceType?>(null) }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var nameServices by remember { mutableStateOf("") }
    var customPrice by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }
    var numberStaff by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null) ?: "unknown_user"
    val userName = sharedPref.getString("username", null) ?: "unknown_user"
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            HeaderSection()
        }

        // Service Type Selection
        item {
            ServiceTypeSection(
                serviceTypes = serviceTypes,
                selectedType = selectedType,
                onTypeSelected = { type ->
                    selectedType = type
                    selectedService = null
                    viewModel.fetchServicesByType(type.id.toString())
                }
            )
        }

        // Service Selection
        item {
            ServiceSelectionSection(
                services = services,
                selectedService = selectedService,
                onServiceSelected = { selectedService = it },
                enabled = selectedType != null
            )
        }

        // Service Details Form
        item {
            ServiceDetailsSection(
                nameServices = nameServices,
                onNameChange = { nameServices = it },
                customPrice = customPrice,
                onPriceChange = { input ->
                    if (input.all { it.isDigit() }) customPrice = input
                },
                numberStaff = numberStaff,
                onNumberStaffChange = { input ->
                    if (input.all { it.isDigit() }) numberStaff = input
                },
                customDescription = customDescription,
                onDescriptionChange = { customDescription = it }
            )
        }

        // Register Button
        item {
            RegisterButtonSection(
                enabled = selectedType != null && selectedService != null && nameServices.isNotBlank(),
                onRegister = {
                    val data = ProviderServicesInsert(
                        serviceId = selectedService!!.id,
                        name = nameServices,
                        providerId = userId,
                        price = customPrice.toString(),
                        description = customDescription,
                        numberStaff = numberStaff.toInt(),
                    )

                    if (selectedService != null && userName.isNotBlank()) {
                        viewModel.registerService(
                            service = data,
                            onSuccess = {
                                showSuccess = true
                                errorMessage = null
                            },
                            onError = { msg ->
                                showSuccess = false
                                errorMessage = msg
                            }
                        )
                    } else {
                        errorMessage = "Vui lòng chọn dịch vụ và nhập tên người dùng"
                    }
                }
            )
        }

        // Status Messages
        item {
            StatusSection(
                showSuccess = showSuccess,
                errorMessage = errorMessage
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Register Service",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Đăng ký dịch vụ mới",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Thêm dịch vụ của bạn để khách hàng có thể đặt lịch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServiceTypeSection(
    serviceTypes: List<ServiceType>,
    selectedType: ServiceType?,
    onTypeSelected: (ServiceType) -> Unit
) {
    FormCard(
        title = "Loại dịch vụ",
        icon = Icons.AutoMirrored.Filled.List,
        description = "Chọn loại dịch vụ bạn muốn cung cấp"
    ) {
        DropdownMenuComboBox(
            label = "Chọn loại dịch vụ",
            items = serviceTypes,
            selectedItem = selectedType,
            onItemSelected = onTypeSelected,
            itemLabel = { it.name },
            icon = Icons.AutoMirrored.Filled.List
        )
    }
}

@Composable
private fun ServiceSelectionSection(
    services: List<Service>,
    selectedService: Service?,
    onServiceSelected: (Service) -> Unit,
    enabled: Boolean
) {
    FormCard(
        title = "Dịch vụ cụ thể",
        icon = Icons.Default.Build,
        description = "Chọn dịch vụ cụ thể từ danh sách"
    ) {
        DropdownMenuComboBox(
            label = if (enabled) "Chọn dịch vụ" else "Vui lòng chọn loại dịch vụ trước",
            items = services,
            selectedItem = selectedService,
            onItemSelected = onServiceSelected,
            itemLabel = { it.name },
            icon = Icons.Default.Build,
            enabled = enabled
        )
    }
}

@Composable
private fun ServiceDetailsSection(
    nameServices: String,
    onNameChange: (String) -> Unit,
    customPrice: String,
    onPriceChange: (String) -> Unit,
    numberStaff: String,
    onNumberStaffChange: (String) -> Unit,
    customDescription: String,
    onDescriptionChange: (String) -> Unit
) {
    FormCard(
        title = "Chi tiết dịch vụ",
        icon = Icons.Default.Edit,
        description = "Nhập thông tin chi tiết về dịch vụ của bạn"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CustomTextField(
                value = nameServices,
                onValueChange = onNameChange,
                label = "Tên dịch vụ",
                icon = Icons.Default.Star,
                placeholder = "Ví dụ: Cắt tóc nam cơ bản"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomTextField(
                    value = customPrice,
                    onValueChange = onPriceChange,
                    label = "Giá (VNĐ)",
                    icon = Icons.Default.Star,
                    placeholder = "100000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                CustomTextField(
                    value = numberStaff,
                    onValueChange = onNumberStaffChange,
                    label = "Số nhân viên",
                    icon = Icons.Default.Star,
                    placeholder = "30",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            CustomTextField(
                value = customDescription,
                onValueChange = onDescriptionChange,
                label = "Mô tả dịch vụ",
                icon = Icons.Default.Info,
                placeholder = "Mô tả chi tiết về dịch vụ...",
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
private fun RegisterButtonSection(
    enabled: Boolean,
    onRegister: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onRegister,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Register",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng ký dịch vụ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vui lòng điền đầy đủ thông tin để đăng ký",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusSection(
    showSuccess: Boolean,
    errorMessage: String?
) {
    if (showSuccess) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Đăng ký dịch vụ thành công!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    errorMessage?.let { message ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Lỗi: $message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun FormCard(
    title: String,
    icon: ImageVector,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownMenuComboBox(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) where T : Any {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}
