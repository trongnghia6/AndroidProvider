package com.example.testappcc.data.model

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.testappcc.core.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class Bookings(
    val id: Int,
    @SerialName("customer_id")
    val customerId: String,
    val description: String?,
    val status: String,
    val location: String,
    @SerialName("start_at")
    val startAt: Instant,
    @SerialName("end_at")
    val endAt: Instant,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("name_services")
    val nameServices: String,
    @SerialName("duration_minutes")
    val durationMinutes: Int?,
    @SerialName("custom_price")
    val customPrice: Double?,
    @SerialName("name_customer")
    val customerName: String?
)
suspend fun fetchBookingsByProvider(providerId: String): List<Bookings> {
    return try {
        val result = supabase.postgrest.rpc(
            function = "get_bookings_by_provider",
            parameters = buildJsonObject {
                put("provider_id_input", JsonPrimitive(providerId))
            }
        )
        val bookings = result.decodeList<Bookings>()
        Log.d("Supabase", "Fetched ${bookings.size} bookings for provider $providerId")
        bookings
    } catch (e: Exception) {
        Log.e("Supabase", "Failed to fetch bookings for provider $providerId", e)
        emptyList() // hoặc throw e nếu bạn muốn propagate lỗi
    }
}



