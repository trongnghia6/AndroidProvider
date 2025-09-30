package com.example.providerapp.data.repository

import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.Service
import com.example.providerapp.data.model.ServiceType
import io.github.jan.supabase.postgrest.from

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