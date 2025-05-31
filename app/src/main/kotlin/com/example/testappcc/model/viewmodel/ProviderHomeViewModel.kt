package com.example.testappcc.model.viewmodel

import android.util.Log
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testappcc.core.supabase
import com.example.testappcc.data.model.Bookings
import com.example.testappcc.data.model.fetchBookingsByProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch


class ProviderHomeViewModel : ViewModel() {

    var bookings by mutableStateOf<List<Bookings>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var pendingTasks by mutableStateOf<List<Bookings>>(emptyList())
        private set

    fun loadBookings(providerId: String) {
        viewModelScope.launch {
            try {
                errorMessage = null
                val allBookings = fetchBookingsByProvider(providerId)
                // Lọc những task có trạng thái "pending"
                val filterBookings = allBookings.filter { it.status == "confirmed" }
                bookings = filterBookings // Hoặc lọc theo ngày nếu cần
            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("ProviderHomeVM", "Error loading bookings", e)
            }
        }
    }
    fun loadPendingTasks(providerId: String) {
        viewModelScope.launch {
            try {
                errorMessage = null
                val allBookings = fetchBookingsByProvider(providerId)
                // Lọc những task có trạng thái "pending"
                val pending = allBookings.filter { it.status == "pending" }
                pendingTasks = pending
            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("ProviderHomeVM", "Error loading Pendingtask", e)
            }
        }
    }
    fun acceptTask(taskId: Int, providerId: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest.from("bookings").update(
                    {
                        set("status","confirmed")
                    }){
                        filter {
                            eq("id", taskId)
                        }
                    }
                // Refresh pending tasks
                loadPendingTasks(providerId)
            } catch (e: Exception) {
                errorMessage = e.message
                Log.d("UpdateTask", "$e")
            }
        }
    }

    fun rejectTask(taskId: Int, providerId: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest.from("bookings").update(
                    {
                            set("status","cancelled")
                            }
                    ){
                        filter {
                            eq("id", taskId)
                        }
                    }
                // Refresh pending tasks
                loadPendingTasks(providerId)
            } catch (e: Exception) {
                errorMessage = e.message
                Log.d("UpdateTask", "$e")
            }
        }
    }

}
