package com.example.providerapp.model.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.ProviderServices
import com.example.providerapp.data.model.ProviderServicesInsert
import com.example.providerapp.data.model.Service
import com.example.providerapp.data.model.ServiceType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class RegisterServiceViewModel : ViewModel() {

    var serviceTypes by mutableStateOf<List<ServiceType>>(emptyList())
        private set

    var services by mutableStateOf<List<Service>>(emptyList())
        private set

    var providerService by mutableStateOf(ProviderServices.EMPTY)
        private set

    init {
        viewModelScope.launch {
            serviceTypes = supabase.from("service_types").select().decodeList()
        }
    }

    fun fetchServicesByType(typeId: String) {
        viewModelScope.launch {
            services = supabase.from("services")
                .select(){
                    filter {
                        eq("service_type_id", typeId)
                    }
                }
                .decodeList()
        }
    }
    fun registerService(service: ProviderServicesInsert, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = supabase.from("provider_services")
                    .insert(service)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Lỗi không xác định")
                Log.d("RegisterServices", "${e.message}")
            }
        }
    }
}
