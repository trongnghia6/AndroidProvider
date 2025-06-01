package com.example.testappcc.model.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testappcc.core.supabase
import com.example.testappcc.data.model.Bookings
import com.example.testappcc.data.model.fetchBookingsByProvider
import com.example.testappcc.data.model.geocodeAddress
import com.google.android.gms.location.LocationServices
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
                loadBookings(providerId)
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
                loadBookings(providerId)
            } catch (e: Exception) {
                errorMessage = e.message
                Log.d("UpdateTask", "$e")
            }
        }
    }
    fun checkLocationDistance(
        context: Context,
        reminderAddress: String,
        accessToken: String,
        onResult: (Boolean) -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(false)
            return
        }
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        fusedClient.lastLocation.addOnSuccessListener { location ->
            val currentLat = location?.latitude
            val currentLng = location?.longitude

            if (currentLat == null || currentLng == null) {
                onResult(false)
                return@addOnSuccessListener
            }

            geocodeAddress(reminderAddress, accessToken) { lat, lng, error ->
                if (lat != null && lng != null) {
                    val current = Location("").apply {
                        latitude = currentLat
                        longitude = currentLng
                    }
                    val saved = Location("").apply {
                        latitude = lat
                        longitude = lng
                    }

                    val distance = current.distanceTo(saved)
                    onResult(distance > 500) // true nếu cách xa > 500m
                } else {
                    onResult(false)
                }
            }
        }
    }


}
