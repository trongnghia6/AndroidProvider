package com.example.providerapp.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.data.model.Service
import com.example.providerapp.data.model.ServiceType
import com.example.providerapp.data.repository.ServiceTypeRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // Trạng thái loading
    val isLoading: MutableState<Boolean> = mutableStateOf(false)

    var serviceTypes by mutableStateOf<List<ServiceType>>(emptyList())
        private set

    var services by mutableStateOf<List<Service>>(emptyList())
        private set

    var selectedTypeId by mutableStateOf<Int?>(null)

    init {
        val repository = ServiceTypeRepository()
        viewModelScope.launch {
            isLoading.value = true
            serviceTypes = repository.getAllServiceTypes()
            isLoading.value = false
        }
    }

    fun loadServicesByType(typeId: Int) {
        isLoading.value = true
        viewModelScope.launch {
            selectedTypeId = typeId
            val repository = ServiceTypeRepository()
            services = repository.getServicesByType(typeId)
            isLoading.value = false
        }
    }

    fun clearSelection() {
        selectedTypeId = null
        services = emptyList()
    }
}