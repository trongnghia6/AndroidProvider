package com.example.providerapp.core.paypal

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PayPalResult(
    val status: String,
    val orderId: String? = null
)

// Callback interface for handling PayPal payment results
interface PayPalPaymentCallback {
    fun onPaymentSuccess(orderId: String)
    fun onPaymentCancelled()
    fun onPaymentFailed(error: String)
}

object PayPalDeepLinkHandler {
    private val _paypalResult = MutableStateFlow<PayPalResult?>(null)
    val paypalResult: StateFlow<PayPalResult?> = _paypalResult.asStateFlow()

    private val processedOrderIds = mutableSetOf<String>()
    private var paymentCallback: PayPalPaymentCallback? = null
    
    fun setPaymentCallback(callback: PayPalPaymentCallback?) {
        paymentCallback = callback
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri != null && uri.scheme == "myapp" && uri.host == "paypal-return") {
            Log.d("PayPalDeepLinkHandler", "PayPal deep link received: $uri")

            val status = uri.getQueryParameter("status")
            val orderId = uri.getQueryParameter("orderId")
            val errorMessage = uri.getQueryParameter("error")

            Log.d("PayPalDeepLinkHandler", "PayPal status: $status, orderId: $orderId, error: $errorMessage")

            // Prevent processing the same order multiple times
            if (orderId != null && processedOrderIds.contains(orderId)) {
                Log.d("PayPalDeepLinkHandler", "Order $orderId already processed, ignoring duplicate")
                return
            }

            if (orderId != null) {
                processedOrderIds.add(orderId)
            }

            val result = PayPalResult(
                status = status ?: "unknown",
                orderId = orderId
            )
            
            _paypalResult.value = result
            
            // Call the callback based on status
            when (status) {
                "success", "approved" -> {
                    if (orderId != null) {
                        Log.d("PayPalDeepLinkHandler", "Payment successful, calling onPaymentSuccess")
                        paymentCallback?.onPaymentSuccess(orderId)
                    } else {
                        Log.e("PayPalDeepLinkHandler", "Payment success but no orderId")
                        paymentCallback?.onPaymentFailed("Payment success but no orderId")
                    }
                }
                "cancelled", "cancel" -> {
                    Log.d("PayPalDeepLinkHandler", "Payment cancelled by user")
                    paymentCallback?.onPaymentCancelled()
                }
                else -> {
                    val message = errorMessage ?: "Payment failed with status: $status"
                    Log.e("PayPalDeepLinkHandler", "Payment failed: $message")
                    paymentCallback?.onPaymentFailed(message)
                }
            }
        }
    }

    fun clearResult() {
        _paypalResult.value = null
    }

    fun clearProcessedOrders() {
        processedOrderIds.clear()
    }
}
