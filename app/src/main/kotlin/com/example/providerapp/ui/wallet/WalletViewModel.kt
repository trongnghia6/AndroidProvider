package com.example.providerapp.ui.wallet

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.core.network.WithDrawResponse
import com.example.providerapp.core.network.RetrofitInstance
import com.example.providerapp.core.network.WithDrawRequest
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.Transaction
import com.example.providerapp.data.repository.BookingPaypalRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.providerapp.core.paypal.PayPalPaymentCallback
import com.example.providerapp.core.paypal.PayPalDeepLinkHandler
import com.example.providerapp.data.model.UserWithPayPal
import com.example.providerapp.data.model.WalletData

class WalletViewModel : ViewModel(), PayPalPaymentCallback {
    
    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()
    
    private val _isLoadingTransactions = MutableStateFlow(false)
    val isLoadingTransactions: StateFlow<Boolean> = _isLoadingTransactions.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // PayPal Web Payment states
    private val _paypalPaymentUrl = MutableStateFlow<String?>(null)
    val paypalPaymentUrl: StateFlow<String?> = _paypalPaymentUrl.asStateFlow()
    
    private val _pendingDepositAmount = MutableStateFlow<Double?>(null)
    val pendingDepositAmount: StateFlow<Double?> = _pendingDepositAmount.asStateFlow()
    
    private val _pendingDepositUserId = MutableStateFlow<String?>(null)
    val pendingDepositUserId: StateFlow<String?> = _pendingDepositUserId.asStateFlow()
    
    init {
        // Set this ViewModel as the payment callback
        PayPalDeepLinkHandler.setPaymentCallback(this)
    }
    
    fun loadWalletData(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                // Load wallet balance
                val userResult = supabase.from("users")
                    .select(columns = Columns.list("wallet_balance")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<WalletData>()
                Log.d("WalletViewModel", "User wallet data: $userResult")
                
                _walletBalance.value = userResult?.walletBalance!!
                Log.d("WalletViewModel", "Loaded wallet balance: ${_walletBalance.value}")
                
                // Load transaction history
                loadTransactionHistory(userId)
                
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error loading wallet data", e)
                _errorMessage.value = "Không thể tải thông tin ví: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadTransactionHistory(userId: String) {
        viewModelScope.launch {
            try {
                _isLoadingTransactions.value = true
                
                // Load wallet transactions from a hypothetical wallet_transactions table
                // For now, we'll create some mock data
                val mockTransactions = supabase.postgrest.from("wallet_transaction").select {
                    filter { eq("user_id", userId) }
                    order("created_at", order = Order.DESCENDING)
                }

                _transactions.value = mockTransactions.decodeList<WalletTransaction>()
                Log.d("WalletViewModel", "Loaded ${_transactions.value} transactions")
                
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error loading transaction history", e)
                _errorMessage.value = "Không thể tải lịch sử giao dịch: ${e.message}"
            } finally {
                _isLoadingTransactions.value = false
            }
        }
    }
    
    fun depositMoney(userId: String, amount: Double) {
        viewModelScope.launch {
            try {
                // Validate amount
                if (amount <= 0) {
                    _errorMessage.value = "Số tiền nạp phải lớn hơn 0"
                    return@launch
                }
                
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                // Create PayPal order for deposit
                val bookingPaypalRepo = BookingPaypalRepository(RetrofitInstance.api)
                
                // Create PayPal order
                val orderResponse = bookingPaypalRepo.createOrder(
                    amount = amount.toString(),
                    currency = "USD"
                )

                // Find approval URL from PayPal response
                val approvalUrl = orderResponse.links.find { it.rel == "approve" }?.href
                val orderId = orderResponse.id
                
                if (approvalUrl != null) {
                    // Store pending deposit info
                    _pendingDepositAmount.value = amount
                    _pendingDepositUserId.value = userId
                    
                    // Preload PayPal URL to improve loading speed
                    preloadPayPalUrl(approvalUrl)
                    
                    // Set PayPal payment URL to open in Chrome Custom Tabs
                    _paypalPaymentUrl.value = approvalUrl
                    
                    Log.d("WalletViewModel", "PayPal payment URL created: $approvalUrl")

                } else {
                    _errorMessage.value = "Không thể tạo liên kết thanh toán PayPal"
                }
                
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error creating PayPal order", e)
                _errorMessage.value = "Không thể tạo đơn hàng PayPal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun withdrawMoney(userId: String, amount: Double) {
        viewModelScope.launch {
            try {
                // Validate amount
                if (amount <= 0) {
                    _errorMessage.value = "Số tiền rút phải lớn hơn 0"
                    return@launch
                }
                
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                // Check if user has sufficient balance
                if (_walletBalance.value < amount) {
                    _errorMessage.value = "Số dư không đủ để rút tiền"
                    return@launch
                }
                
                // Get user's PayPal email
                val userResult = supabase.from("users")
                    .select(columns = Columns.list("paypal_email")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<UserWithPayPal>()
                
                val paypalEmail = userResult.paypalEmail
                
                if (paypalEmail.isEmpty()) {
                    _errorMessage.value = "Chưa liên kết PayPal. Vui lòng cập nhật email PayPal trong hồ sơ."
                    return@launch
                }
                
                // Store pending withdrawal info
                _pendingDepositAmount.value = -amount // Negative amount for withdrawal
                _pendingDepositUserId.value = userId
                
                // Create PayPal withdraw request
                val withDrawRequest = WithDrawRequest(
                    receiver = paypalEmail,
                    amount = amount.toString(),
                    currency = "USD"
                )
                
                // Send withdraw request and handle result immediately
                val withDrawResponse = RetrofitInstance.api.createWithDraw(withDrawRequest)
                
                // Log PayPal transaction IDs
                Log.d("WalletViewModel", "PayPal withdraw response: $withDrawResponse")
                
                // Update wallet balance
                updateWalletBalance(userId, -amount, "withdraw")

                // Add transaction record with PayPal transaction ID
                addWalletTransaction(
                    userId = userId,
                    type = "withdraw",
                    amount = amount,
                    transactionId = withDrawResponse.withDrawId
                )
                    
                _successMessage.value = "Rút tiền thành công"
                Log.d("WalletViewModel", "Withdrawal successful. PayPal will create 2 transactions: Payment and Mass Payout")
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error withdrawing money", e)
                _errorMessage.value = "Không thể rút tiền: ${e.message}"
                
                // Clear pending info on error
                _pendingDepositAmount.value = null
                _pendingDepositUserId.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun updateWalletBalance(userId: String, amount: Double, type: String) {
        viewModelScope.launch {
            try {
                // Update wallet balance in database
                supabase.from("users")
                    .update(mapOf("wallet_balance" to (_walletBalance.value + amount))) {
                        filter { eq("id", userId) }
                    }
                
                // Update local state
                _walletBalance.value += amount
                
                // Add transaction to history
                val newTransaction = WalletTransaction(
                    id = System.currentTimeMillis().toString(),
                    type = type,
                    amount = kotlin.math.abs(amount),
                    date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    status = "completed"
                )
                
                _transactions.value = listOf(newTransaction) + _transactions.value
                
                Log.d("WalletViewModel", "Updated wallet balance for user $userId")
                
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error updating wallet balance", e)
                _errorMessage.value = "Không thể cập nhật số dư: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearPaypalPaymentUrl() {
        _paypalPaymentUrl.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    

    private fun addWalletTransaction(
        userId: String,
        type: String,
        amount: Double,
        transactionId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val transaction = WalletTransaction(
                    id = transactionId ?: System.currentTimeMillis().toString(),
                    userId = userId,
                    type = type,
                    amount = amount,
                    date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    status = "completed",
                )
                
                supabase.from("wallet_transaction").insert(transaction)
                
                // Refresh transaction history
                loadTransactionHistory(userId)
                
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error adding wallet transaction", e)
            }
        }
    }

    // Preload PayPal URL để tăng tốc độ
    private fun preloadPayPalUrl(approvalUrl: String) {
        viewModelScope.launch {
            try {
                Log.d("PayPalViewModel", "🔄 Preloading PayPal URL: $approvalUrl")
                // Có thể thêm logic preload ở đây
                // Ví dụ: cache URL hoặc warmup CustomTabs
            } catch (e: Exception) {
                Log.e("PayPalViewModel", "❌ Error preloading PayPal URL: ${e.message}", e)
            }
        }
    }

    // Mở PayPal URL với tối ưu tốc độ (public để sử dụng từ UI)
    fun openPaypalUrlFast(context: Context, approvalUrl: String) {
        try {
            Log.d("PayPalViewModel", "🚀 Fast opening PayPal URL: $approvalUrl")

            // Sử dụng CustomTabs với tối ưu tốc độ
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultShareMenuItemEnabled(false)
                .build()

            // Thêm flags để tăng tốc độ
            customTabsIntent.intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            customTabsIntent.launchUrl(context, approvalUrl.toUri())
            Log.d("PayPalViewModel", "✅ Successfully launched PayPal URL with fast mode")
        } catch (e: Exception) {
            Log.e("PayPalViewModel", "❌ Error opening PayPal URL fast: ${e.message}", e)
            // Fallback to standard method
            openPaypalUrl(context, approvalUrl)
        }
    }

    // Phương thức cũ để fallback
    private fun openPaypalUrl(context: Context, approvalUrl: String) {
        try {
            Log.d("PayPalViewModel", "🔄 Attempting to open PayPal URL: $approvalUrl")
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, approvalUrl.toUri())
            Log.d("PayPalViewModel", "✅ Successfully launched PayPal URL")
        } catch (e: Exception) {
            Log.e("PayPalViewModel", "❌ Error opening PayPal URL: ${e.message}", e)
            try {
                val intent = Intent(Intent.ACTION_VIEW, approvalUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d("PayPalViewModel", "✅ Fallback: Opened PayPal URL with ACTION_VIEW")
            } catch (fallbackException: Exception) {
                Log.e("PayPalViewModel", "❌ Fallback also failed: ${fallbackException.message}", fallbackException)
            }
        }
    }
    
    // PayPalPaymentCallback implementation
    override fun onPaymentSuccess(orderId: String) {
        Log.d("WalletViewModel", "PayPal payment successful: $orderId")
        viewModelScope.launch {
            try {
                val bookingPaypalRepo = BookingPaypalRepository(RetrofitInstance.api)
                val pendingAmount = _pendingDepositAmount.value
                val pendingUserId = _pendingDepositUserId.value
                
                // Only handle deposit in PayPal callback
                if (pendingAmount != null && pendingUserId != null && pendingAmount > 0) {
                    // Handle deposit
                    val captureResponse = bookingPaypalRepo.captureOrder(orderId)

                    if (captureResponse.status == "COMPLETED") {
                        // Update wallet balance
                        updateWalletBalance(pendingUserId, pendingAmount, "deposit")
                        
                        // Add transaction record
                        addWalletTransaction(pendingUserId, "deposit", pendingAmount)
                        
                        _successMessage.value = "Nạp tiền thành công"
                    } else {
                        _errorMessage.value = "Thanh toán chưa hoàn tất"
                    }
                } else {
                    _errorMessage.value = "Không tìm thấy thông tin giao dịch hoặc không phải giao dịch nạp tiền"
                }
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error processing successful payment", e)
                _errorMessage.value = "Lỗi xử lý giao dịch: ${e.message}"
            } finally {
                // Clear states
                _paypalPaymentUrl.value = null
                _pendingDepositAmount.value = null
                _pendingDepositUserId.value = null
            }
        }
    }
    
    override fun onPaymentCancelled() {
        Log.d("WalletViewModel", "PayPal payment cancelled")
        val isWithdraw = (_pendingDepositAmount.value ?: 0.0) < 0
        _errorMessage.value = if (isWithdraw) "Rút tiền đã bị hủy" else "Thanh toán đã bị hủy"
        
        // Clear states
        _paypalPaymentUrl.value = null
        _pendingDepositAmount.value = null
        _pendingDepositUserId.value = null
    }
    
    override fun onPaymentFailed(error: String) {
        val isWithdraw = (_pendingDepositAmount.value ?: 0.0) < 0
        Log.e("WalletViewModel", "PayPal ${if (isWithdraw) "withdrawal" else "payment"} failed: $error")
        _errorMessage.value = if (isWithdraw) 
            "Rút tiền thất bại: $error" 
        else 
            "Thanh toán thất bại: $error"
        
        // Clear states
        _paypalPaymentUrl.value = null
        _pendingDepositAmount.value = null
        _pendingDepositUserId.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clear the payment callback when ViewModel is cleared
        PayPalDeepLinkHandler.setPaymentCallback(null)
    }
}
