package com.example.providerapp.data.model

import com.example.providerapp.core.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ServiceType(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
)

@Serializable
data class Service(
    val id: Int,
    @SerialName("service_type_id")
    val serviceTypeId: Int,
    val name: String,
    val description: String? = null,
    @SerialName("number_staff")
    val numberStaff: Int? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
)
@Serializable
data class ProviderServices(
    val id: Int = 0,
    val name: String = "",
    @SerialName("service_id")
    val serviceId: Int = 0,
    @SerialName("provider_id")
    val providerId: String = "",
    @SerialName("custom_price")
    val price: Int = 0,
    @SerialName("custom_description")
    val description: String = "",
    @SerialName("number_staff")
    val numberStaff: Int? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
){
    companion object {
        val EMPTY = ProviderServices(0, "",0, "", 0, "", 0, true)
    }
}

@Serializable
data class ProviderServicesInsert(
    val name: String,
    @SerialName("service_id")
    val serviceId: Int,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("custom_price")
    val price: String,
    @SerialName("custom_description")
    val description: String,
    @SerialName("number_staff")
    val numberStaff: Int?,
)


class ServiceTypeRepository {

    suspend fun getAllServiceTypes(): List<ServiceType> {
        return supabase.from("service_types")
            .select()
            .decodeList()
    }

    suspend fun getServicesByType(serviceTypeId: Int): List<Service> {
        return supabase.from("services")
            .select(){
                filter {
                    eq("service_type_id", serviceTypeId)
                }
            }
            .decodeList()
    }
}


