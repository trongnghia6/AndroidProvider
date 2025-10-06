package com.example.providerapp.ui.wallet

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.providerapp.core.paypal.PayPalWebHelper
import com.example.providerapp.ui.wallet.WalletViewModel
import java.text.NumberFormat
import java.util.*
import androidx.core.net.toUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val providerId = sharedPref.getString("user_id", "") ?: ""
    
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var depositAmount by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }
    val balance by viewModel.walletBalance.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    // Load wallet data when screen opens
    LaunchedEffect(providerId) {
        if (providerId.isNotEmpty()) {
            viewModel.loadWalletData(providerId)
        }
    }
    
    // Handle PayPal payment URL - using collectAsState to properly observe StateFlow
    val paymentUrl by viewModel.paypalPaymentUrl.collectAsState()
    
    // Debug log to track StateFlow changes
    LaunchedEffect(paymentUrl) {
        Log.d("WalletScreen", "Payment URL StateFlow changed: $paymentUrl")
    }
    
    LaunchedEffect(paymentUrl) {
        if (paymentUrl != null) {
            Log.d("WalletScreen", "Opening PayPal URL: $paymentUrl")
            try {
                PayPalWebHelper.openPayPalWeb(context, paymentUrl!!)
                viewModel.clearPaypalPaymentUrl()
                Log.d("WalletScreen", "PayPal URL opened successfully in Chrome Custom Tabs")
            } catch (e: Exception) {
                Log.e("WalletScreen", "Error opening PayPal URL: ${e.message}")
                viewModel.clearPaypalPaymentUrl()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ví của tôi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wallet Balance Card
            item {
                WalletBalanceCard(
                    balance = balance,
                    isLoading = isLoading
                )
            }
            
            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Deposit Button
                    Button(
                        onClick = { showDepositDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nạp tiền",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nạp tiền")
                    }
                    
                    // Withdraw Button
                    OutlinedButton(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = balance > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Rút tiền",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rút tiền")
                    }
                }
            }
            
            // Transaction History
            item {
                val transactions by viewModel.transactions.collectAsState()
                val isLoadingTransactions by viewModel.isLoadingTransactions.collectAsState()
                
                TransactionHistorySection(
                    transactions = transactions,
                    isLoading = isLoadingTransactions
                )
            }
            
            // Test Deep Link Button (for development)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Simulate PayPal success deep link for testing
                            val mockUri = Uri.parse("myapp://paypal-return?status=success&orderId=TEST_ORDER_123")
                            com.example.providerapp.core.paypal.PayPalDeepLinkHandler.handleDeepLink(mockUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Test PayPal Success (Dev)")
                    }
                    
                    Button(
                        onClick = {
                            // Test opening PayPal URL directly
                            try {
                                val testUrl = "https://www.sandbox.paypal.com/checkoutnow?token=TEST_TOKEN_123"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                Log.d("WalletScreen", "Test PayPal URL opened: $testUrl")
                            } catch (e: Exception) {
                                Log.e("WalletScreen", "Error opening test PayPal URL: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Test Open PayPal URL (Dev)")
                    }
                }
            }
        }
        
        // Deposit Dialog
        if (showDepositDialog) {
            DepositDialog(
                amount = depositAmount,
                onAmountChange = { depositAmount = it },
                onConfirm = { amount ->
                    viewModel.depositMoney(providerId, amount.toDoubleOrNull() ?: 0.0)
                    showDepositDialog = false
                    depositAmount = ""
                },
                onDismiss = { 
                    showDepositDialog = false
                    depositAmount = ""
                }
            )
        }
        
        // Withdraw Dialog
        if (showWithdrawDialog) {
            WithdrawDialog(
                amount = withdrawAmount,
                onAmountChange = { withdrawAmount = it },
                onConfirm = { amount ->
                    Log.d("WalletViewModel", "Withdrawing amount: $amount")
                    viewModel.withdrawMoney(providerId, amount.toDoubleOrNull() ?: 0.0)
                    showWithdrawDialog = false
                    withdrawAmount = ""
                },
                onDismiss = { 
                    showWithdrawDialog = false
                    withdrawAmount = ""
                },
                maxAmount = balance
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun WalletBalanceCard(
    balance: Double,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Số dư hiện tại",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${String.format("%,.0f", balance)} VNĐ",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (balance < 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "⚠️ Số dư âm. Tài khoản sẽ bị khóa nếu dưới -500,000 VNĐ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionHistorySection(
    transactions: List<WalletTransaction>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch sử giao dịch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No transactions",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa có giao dịch nào",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transactions.take(10).forEach { transaction ->
                        TransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: WalletTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (transaction.type == "deposit") 
                        Icons.Default.Add 
                    else 
                        Icons.Default.Remove,
                    contentDescription = transaction.type,
                    tint = if (transaction.type == "deposit") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (transaction.type == "deposit") "Nạp tiền" else "Rút tiền",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = transaction.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Text(
                text = "${if (transaction.type == "deposit") "+" else "-"}${String.format("%,.0f", transaction.amount)} VNĐ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (transaction.type == "deposit") 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun DepositDialog(
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Nạp tiền vào ví",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow numbers and one decimal point
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onAmountChange(newValue)
                        }
                    },
                    label = { Text("Số tiền (VNĐ)") },
                    placeholder = { Text("Nhập số tiền muốn nạp") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Thanh toán sẽ được xử lý qua PayPal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = { onConfirm(amount) },
                        modifier = Modifier.weight(1f),
                        enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) {
                        Text("Xác nhận")
                    }
                }
            }
        }
    }
}

@Composable
private fun WithdrawDialog(
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    maxAmount: Double
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Rút tiền từ ví",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow numbers and one decimal point
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onAmountChange(newValue)
                        }
                    },
                    label = { Text("Số tiền (VNĐ)") },
                    placeholder = { Text("Nhập số tiền muốn rút") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    supportingText = {
                        Text("Số dư khả dụng: ${String.format("%,.0f", maxAmount)} VNĐ")
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tiền sẽ được chuyển về PayPal của bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = { onConfirm(amount) },
                        modifier = Modifier.weight(1f),
                        enabled = amount.isNotEmpty() && 
                                amount.toDoubleOrNull() != null && 
                                amount.toDoubleOrNull()!! > 0 &&
                                amount.toDoubleOrNull()!! <= maxAmount
                    ) {
                        Text("Xác nhận")
                    }
                }
            }
        }
    }
}

// Data class for wallet transactions
@Serializable
data class WalletTransaction(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    val type: String, // "deposit" or "withdraw"
    val amount: Double,
    @SerialName("created_at")
    val date: String,
    val status: String ?= "completed"
)
