package com.example.providerapp.data.model.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.providerapp.data.model.Bookings
import com.example.providerapp.core.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

@Composable
fun RenderHome(){
    var items by remember { mutableStateOf<List<Bookings>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val fetchedItems = supabase.from("bookings")
                .select(columns = Columns.list("service_type"))
                .decodeList<Bookings>()
            items = fetchedItems
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
}