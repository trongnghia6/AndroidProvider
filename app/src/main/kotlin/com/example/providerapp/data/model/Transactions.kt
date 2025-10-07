package com.example.providerapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Transactions {
    @SerialName("booking_id")
    var bookingId: Int? = null
    @SerialName("payment_method")
    var paymentMethod: String? = null
    var amount: Double? = null
}