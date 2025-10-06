package com.example.providerapp.data.model

import android.util.Log
import com.example.providerapp.core.supabase
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
    @SerialName("number_staff")
    val numberStaff: Int?,
    @SerialName("custom_price")
    val customPrice: Double?,
    @SerialName("name_customer")
    val customerName: String?,
    @SerialName("phone_number")
    val phoneNumber: String?,
)

@Serializable
data class ListBooking(
    val id: Int,
    @SerialName("customer_id")
    val customerId: String,
    val description: String? = null,
    val status: String,
    val location: String? = null,
    @SerialName("start_at")
    val startAt: String? = null,
    @SerialName("end_at")
    val endAt: String? = null,
    @SerialName("provider_service_id")
    val providerServiceId: Int? = null,
    @SerialName("number_workers")
    val numberWorkers: Int? = null,

    // nested list of transactions
    val transactions: List<Transaction> = emptyList(),

    // nested provider service
    @SerialName("provider_services")
    val providerService: ProviderService? = null,
    val users: Users ? = null, // nested user data
)

@Serializable
data class Transaction(
    val id: String,
    @SerialName("booking_id")
    val bookingId: Int? = null,
    @SerialName("provider_services_id")
    val providerServicesId: Int? = null,
    val amount: Double,
    val status: String,
    @SerialName("payment_method")
    val paymentMethod: String? = null,
    @SerialName("capture_id")
    val captureId: String? = null,
    @SerialName("paypal_order_id")
    val paypalOrderId: String? = null,
    @SerialName("payout_id")
    val payoutId: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ProviderService(
    val id: Int,
    val name: String,
    @SerialName("provider_id")
    val providerId: String
)

@Serializable
data class BookingInsert(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("provider_service_id")
    val providerServiceId: Int,
    val status: String,
    val location: String,
    @SerialName("start_at")
    val startAt: String,
    @SerialName("end_at")
    val endAt: String,
    @SerialName("num_workers")
    val numWorkers: Int
)

@Serializable
data class BookingResponse(
    val id: Int,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("provider_service_id")
    val providerServiceId: Int,
    val status: String,
    val location: String,
    @SerialName("start_at")
    val startAt: String,
    @SerialName("end_at")
    val endAt: String,
    @SerialName("num_workers")
    val numWorkers: Int
)

@Serializable
data class BookingWithProvider(
    val id: Int,
    @SerialName("provider_service_id")
    val providerServiceId: Int,
    @SerialName("provider_services")
    val providerServices: ProviderServiceWithUser
)

@Serializable
data class ProviderServiceWithUser(
    val id: Int,
    @SerialName("provider_id")
    val providerId: String,
    val users: UserWithPayPal
)

@Serializable
data class UserWithPayPal(
    @SerialName("paypal_email")
    val paypalEmail: String
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



