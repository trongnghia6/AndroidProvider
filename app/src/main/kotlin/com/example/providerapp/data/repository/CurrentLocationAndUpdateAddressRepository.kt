package com.example.providerapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.providerapp.core.network.MapboxGeocodingService
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrentLocationAndUpdateAddressRepository {
    companion object {
        fun getCurrentLocationAndUpdateAddress(
            context: Context,
            geocodingService: MapboxGeocodingService,
            accessToken: String,
            onAddressFound: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                onError("Quyền vị trí chưa được cấp.")
                Log.d("location","Quyền vị trí chưa được cấp.")
                return
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        fusedLocationClient.removeLocationUpdates(this)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = geocodingService.reverseGeocode(
                                    longitude = location.longitude,
                                    latitude = location.latitude,
                                    accessToken = accessToken
                                )
                                val placeName = response.features.firstOrNull()?.placeName
                                withContext(Dispatchers.Main) {
                                    if (placeName != null) {
                                        onAddressFound(placeName)
                                        Log.d("location", placeName)
                                    } else {
                                        onError("Không tìm thấy địa chỉ.")
                                        Log.d("location", "Không tìm thấy địa chỉ.")
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onError("Lỗi reverse geocoding: ${e.message}")
                                    Log.d("location", "Lỗi reverse geocoding: ${e.message}")
                                }
                            }
                        }
                    } else {
                        onError("Không thể lấy vị trí hiện tại.")
                        Log.d("location", "Không thể lấy vị trí hiện tại.")
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
}