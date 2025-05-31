package com.example.testappcc.presentation.registerservices

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
    var customDuration by remember { mutableStateOf("") } // nếu muốn cho nhập phút
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null) ?: "unknown_user"
    val userName = sharedPref.getString("username", null) ?: "unknown_user"
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            Text("Đăng ký dịch vụ", style = MaterialTheme.typography.headlineSmall)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            DropdownMenuComboBox(
                label = "Loại dịch vụ",
                items = serviceTypes,
                selectedItem = selectedType,
                onItemSelected = {
                    selectedType = it
                    selectedService = null
                    viewModel.fetchServicesByType(it.id.toString())
                },
                itemLabel = { it.name }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            DropdownMenuComboBox(
                label = "Dịch vụ",
                items = services,
                selectedItem = selectedService,
                onItemSelected = { selectedService = it },
                itemLabel = { it.name }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            TextField(
                value = nameServices,
                onValueChange = { nameServices = it },
                label = { Text("Tên dịch vụ") }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            TextField(
                value = customPrice,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) customPrice = input
                },
                label = { Text("Giá") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            TextField(
                value = customDuration,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) customDuration = input
                },
                label = { Text("Số phút") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            TextField(
                value = customDescription,
                onValueChange = { customDescription = it },
                label = { Text("Mô tả") }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Button(
                onClick = {
                    val data = ProviderServicesInsert(
                        serviceId = selectedService!!.id,
                        name = nameServices,
                        providerId = userId,
                        price = customPrice.toString(),
                        description = customDescription,
                        durationMinutes = customDuration.toInt(),
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
                },
                enabled = selectedType != null && selectedService != null
            ) {
                Text("Đăng ký")
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            if (showSuccess) {
                Text("Đăng ký thành công!", color = Color.Green)
            }
        }

        item {
            errorMessage?.let {
                Text("Lỗi: $it", color = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownMenuComboBox(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit
) where T : Any {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedItem?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)

        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text((itemLabel(item))) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

