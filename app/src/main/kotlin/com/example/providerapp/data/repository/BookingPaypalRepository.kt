package com.example.providerapp.data.repository

import android.util.Log
import com.example.providerapp.core.network.*
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.BookingInsert
import com.example.providerapp.data.model.BookingResponse
import com.example.providerapp.data.model.Transaction
import io.github.jan.supabase.postgrest.from
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime


class BookingPaypalRepository(private val api: ApiService) {

    suspend fun createOrder(amount: String, currency: String = "USD"): OrderResponse {
        val request = CreateOrderRequest(amount, currency)
        return api.createOrder(request)
    }

    suspend fun captureOrder(orderId: String): CaptureResponse {
        return api.captureOrder(orderId)
    }

    suspend fun getOrderDetails(orderId: String): OrderDetailsResponse {
        return api.getOrderDetails(orderId)
    }

    suspend fun refundCapture(captureId: String, amount: String? = null, reason: String = "Refund request"): RefundResponse {
        val request = RefundRequest(amount, reason)
        return api.refundCapture(captureId, request)
    }



    suspend fun createPendingBooking(
        userId: String,
        providerServiceId: Int,
        address: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime?,
        durationMinutes: Int?,
        total: Double,
        selectedWorkers: String
    ): BookingResponse {
        val startAtDateTime = startDateTime.atOffset(ZoneOffset.ofHours(7))
            ?: OffsetDateTime.now(ZoneOffset.ofHours(7))
        val endAtDateTime = endDateTime?.atOffset(ZoneOffset.ofHours(7))
            ?: startAtDateTime.plusMinutes(durationMinutes?.toLong() ?: 60L)

        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val startAt = startAtDateTime.format(formatter)
        val endAt = endAtDateTime.format(formatter)

        val booking = BookingInsert(
            customerId = userId,
            providerServiceId = providerServiceId,
            status = "pending",
            location = address,
            startAt = startAt,
            endAt = endAt,
            numWorkers = selectedWorkers.toIntOrNull() ?: 1
        )

        val bookingResult = supabase.from("bookings")
            .insert(booking) { 
                select() 
            }

        val insertedBooking = bookingResult.decodeSingle<BookingResponse>()

        val transaction = Transaction(
            id = System.currentTimeMillis().toString(),
            bookingId = insertedBooking.id,
            providerServicesId = providerServiceId,
            amount = total,
            status = "pending",
            paymentMethod = "Paypal",
            createdAt = java.time.LocalDateTime.now().toString()
        )

        supabase.from("transactions").insert(transaction)

        return insertedBooking
    }

    suspend fun updatePaymentResult(bookingId: Int, captureId: String?, paypalOrderId: String? = null) {
        try {
            // Cập nhật transaction status
            supabase.from("transactions").update(
                mapOf(
                    "status" to "completed",
                    "capture_id" to captureId,
                    "paypal_order_id" to paypalOrderId,
                )
            ) {
                filter { eq("booking_id", bookingId) }
            }

            // Xử lý payout cho nhà cung cấp
//            processPayoutForCompletedOrder(bookingId)

            Log.d("BookingPaypalRepository", "Updated transaction for bookingId=$bookingId with captureId=$captureId, paypalOrderId=$paypalOrderId")
        } catch (e: Exception) {
            Log.e("BookingPaypalRepository", "Error updating transaction for bookingId=$bookingId: ${e.message}", e)
            throw e
        }
    }

    suspend fun updatePayPalOrderId(bookingId: Int, paypalOrderId: String) {
        try {
            supabase.from("transactions").update(
                mapOf(
                    "paypal_order_id" to paypalOrderId,
                    "status" to "pending"
                )
            ) {
                filter { eq("booking_id", bookingId) }
            }
            Log.d("BookingPaypalRepository", "Updated PayPal order ID for bookingId=$bookingId with paypalOrderId=$paypalOrderId")
        } catch (e: Exception) {
            Log.e("BookingPaypalRepository", "Error updating PayPal order ID for bookingId=$bookingId: ${e.message}", e)
            throw e
        }
    }


}