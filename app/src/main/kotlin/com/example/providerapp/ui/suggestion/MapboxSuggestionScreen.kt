package com.example.providerapp.ui.suggestion

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.providerapp.BuildConfig
import com.example.providerapp.core.network.MapboxGeocodingService
import com.example.providerapp.core.network.MapboxPlace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MapboxSuggestionScreen(geocodingService: MapboxGeocodingService) {
    AddressAutoCompleteScreen(geocodingService = geocodingService)
}

@Composable
fun AddressAutoCompleteScreen(geocodingService: MapboxGeocodingService) {
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var places by remember { mutableStateOf(emptyList<MapboxPlace>()) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Gọi API khi query thay đổi
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            delay(300)
            coroutineScope.launch {
                try {
                    val response = geocodingService.searchPlaces(
                        query,
                        BuildConfig.MAPBOX_ACCESS_TOKEN
                    )
                    places = response.features
                    if (response.features.isEmpty()) {
                        snackbarHostState.showSnackbar("Không tìm thấy kết quả.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Lỗi tìm kiếm: ${e.message}")
                }
            }
        } else {
            places = emptyList()
        }
    }

    val state = MapboxSearchState(
        query = query,
        onQueryChange = { newQuery ->
            query = newQuery
            selectedAddress = null // reset nếu người dùng đang gõ mới
        },
        places = places,
        selectedAddress = selectedAddress,
        onAddressSelected = { address ->
            selectedAddress = address
            query = address // ✅ Tự động điền TextField
            places = emptyList() // Ẩn gợi ý
        },
        snackbarHostState = snackbarHostState
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AddressAutoComplete(
            state = state,
            modifier = Modifier.padding(padding).padding(16.dp)
        )
    }
}

@Composable
fun AddressAutoComplete(
    state: MapboxSearchState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TextField(
            value = state.query,
            onValueChange = state.onQueryChange,
            label = { Text("Nhập địa chỉ") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.Black),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Blue,
                unfocusedIndicatorColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hiển thị danh sách gợi ý nếu có
        if (state.places.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .border(1.dp, Color.LightGray)
            ) {
                items(state.places) { place ->
                    Text(
                        text = place.placeName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.onAddressSelected(place.placeName)
                            }
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        state.selectedAddress?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Đã chọn: $it", style = MaterialTheme.typography.bodyMedium)
        }
    }
}


data class MapboxSearchState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val places: List<MapboxPlace>,
    val selectedAddress: String?,
    val onAddressSelected: (String) -> Unit,
    val snackbarHostState: SnackbarHostState
)