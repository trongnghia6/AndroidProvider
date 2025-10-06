package com.example.providerapp.core.paypal

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.core.content.ContextCompat
import com.example.providerapp.R

object PayPalWebHelper {
    fun openPayPalWeb(context: Context, paypalUrl: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setColorScheme(COLOR_SCHEME_DARK)
                .build()

            // Mở URL trong Chrome Custom Tabs
            customTabsIntent.launchUrl(context, Uri.parse(paypalUrl))
        } catch (e: Exception) {
            // Fallback: mở trong trình duyệt mặc định nếu Chrome Custom Tabs không khả dụng
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(paypalUrl))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
