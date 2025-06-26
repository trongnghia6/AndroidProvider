package com.example.providerapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//@Serializable
//data class Services(
//    val id: Int,
//    val name: String,
//    val price: Double,
//    val description: String?,
//    val numWorkers: Int,
//    val serviceTypeId: Int,
//    val specificServiceId: Int,
//    val providerId: String,
//    val createdAt: String? = null,
//    val updatedAt: String? = null
//)
//
@Serializable
data class ServiceProviderInsert(
    val name: String,
    @SerialName("custom_price")
    val price: Double,
    @SerialName("custom_description")
    val description: String?,
    @SerialName("number_staff")
    val numWorkers: Int,
    @SerialName("service_id")
    val specificServiceId: Int,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("is_active")
    val isActive : Boolean = true

)

@Serializable
data class ServiceUpdate(
    val name: String,
    @SerialName("custom_price")
    val price: Double,
    @SerialName("custom_description")
    val description: String?,
    @SerialName("number_staff")
    val numWorkers: Int
)
//
//@Serializable
//data class ServiceTypes(
//    val id: Int,
//    val name: String,
//    val description: String?
//)
//
//@Serializable
//data class SpecificService(
//    val id: Int,
//    val name: String,
//    val serviceTypeId: Int,
//    val description: String?
//)
//
//@Serializable
//data class ServiceWithDetails(
//    val id: Int,
//    val name: String,
//    val price: Double,
//    val description: String?,
//    val numWorkers: Int,
//    val serviceTypeId: Int,
//    val specificServiceId: Int,
//    val providerId: String,
//    val serviceTypeName: String,
//    val specificServiceName: String,
//    val createdAt: String? = null,
//    val updatedAt: String? = null
//)

@Serializable
data class ServiceTypes(
    val name: String
)

@Serializable
data class SpecificService(
    val id: Int = 0,
    val name: String,
    @SerialName("service_type_id")
    val serviceTypeId: Int,
    @SerialName("service_types")
    val serviceType: ServiceTypes? = null
)

@Serializable
data class ServiceWithDetails(
    val id: Int,
    val name: String,
    @SerialName("custom_price")
    val price: Double,
    @SerialName("custom_description")
    val description: String? = null,
    @SerialName("number_staff")
    val numWorkers: Int,
    @SerialName("service_id")
    val specificServiceId: Int,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("services")
    val specificService: SpecificService? = null
) {
    val specificServiceName: String get() = specificService?.name.orEmpty()
    val serviceTypeName: String get() = specificService?.serviceType?.name.orEmpty()
}