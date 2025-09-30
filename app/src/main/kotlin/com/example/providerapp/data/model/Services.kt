package com.example.providerapp.data.model

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
data class ServiceRatingWithUser(
    val id: Int,
    @SerialName("provider_service_id")
    val providerServiceId: Int,
    @SerialName("user_id")
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    val responses: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val users: Users
)

@Serializable
data class ServiceRatingUpdate(
    val responses: String
)


