package com.example.providerapp.data.repository

import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class DistanceRepository {
    companion object {
        // geocode địa chỉ thành tọa độ
        fun geocodeAddress(
            address: String,
            accessToken: String,
            callback: (latitude: Double?, longitude: Double?, error: String?) -> Unit
        ) {
            val geocoding = MapboxGeocoding.builder()
                .accessToken(accessToken)
                .query(address)
                .build()

            geocoding.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    val results = response.body()?.features()
                    if (!results.isNullOrEmpty()) {
                        val firstResult: CarmenFeature = results[0]
                        val point = firstResult.center()
                        if (point != null) {
                            callback(point.latitude(), point.longitude(), null)
                        } else {
                            callback(null, null, "Không lấy được tọa độ")
                        }
                    } else {
                        callback(null, null, "Không tìm thấy kết quả cho địa chỉ")
                    }
                }

                override fun onFailure(
                    call: Call<GeocodingResponse>,
                    t: Throwable
                ) {
                    callback(null, null, t.message)
                }
            })
        }
    }

}