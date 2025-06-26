package com.example.providerapp.core.network

import com.google.gson.annotations.SerializedName

data class MapboxPlace(
    @SerializedName("place_name")
    val placeName: String
)

data class MapboxResponse(
    val features: List<MapboxPlace>
)
