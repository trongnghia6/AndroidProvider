package com.example.providerapp.presentation.search


import androidx.compose.runtime.*
import com.example.providerapp.core.network.MapboxGeocodingService
import com.example.providerapp.data.model.AddressAutoCompleteScreen


@Composable
fun MapboxSuggestionScreen(geocodingService: MapboxGeocodingService) {
    AddressAutoCompleteScreen(geocodingService = geocodingService)
}