package com.example.providerapp.data.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.core.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class Task(
    val id: Int,
    val nameService: String,
    val nameCustomer: String?,
    val phoneNumber: String?,
    val location: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val endTime: String ? = null,
    val startTime: String? = null,
    val status: String,
    val description: String?,
    val customPrice: Double,
)

@Serializable
data class TaskRaw(
    val id: Int,
    @SerialName("name_services")
    val nameService: String,
    @SerialName("name_customer")
    val nameCustomer: String?,
    @SerialName("phone_number")
    val phoneNumber: String?,
    val location: String? = null,
    @SerialName("end_at")
    val endTime: String ? = null,
    @SerialName("start_at")
    val startTime: String? = null,
    val status: String,
    val description: String?,
    @SerialName("custom_price")
    val customPrice: Double,
)
//@Serializable
//data class TaskRaw(
//    val id: Int,
//    @SerialName("service_type")
//    val typeService: String,
//    val location: String? = null,
//    val time: String? = null
//)
@Serializable
data class Booking(
    val id: Int,
    val bookingDate: String,
    val providers: Provider,
    val customers: Customer,
    val services: Service
)

@Serializable
data class Provider(
    val id: Int,
    val name: String,
    val phone: String? = null
)

@Serializable
data class Customer(
    val id: Int,
    val name: String,
    val email: String? = null
)

//@Serializable
//data class Service(
//    val id: Int,
//    val name: String,
//    val serviceTypes: ServiceType
//)
//
//@Serializable
//data class ServiceType(
//    val id: Int,
//    val name: String
//)

class TaskViewModel(private val providerId:  String) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    fun fetchTasks(providerId: String) {
        viewModelScope.launch {
            try {
                val response = supabase.postgrest
                    .rpc(
                        function = "get_bookings_by_provider",
                        parameters = buildJsonObject {
                            put("provider_id_input", JsonPrimitive(providerId))
                        })
                    .decodeList<TaskRaw>()
                Log.e("TaskRender", "$response")
                val tasksList = response.mapNotNull { raw ->
                    Log.e("TaskRender", "$raw")
                    val datetimeStartStr = raw.startTime ?: return@mapNotNull null
                    val datetimeEndStr = raw.endTime ?: return@mapNotNull null
                    try {
                        val systemZone = ZoneId.systemDefault()

                        val odst = OffsetDateTime.parse(datetimeStartStr).atZoneSameInstant(systemZone)
                        val startDate = odst.toLocalDate()
                        val startTime = odst.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

                        val odet = OffsetDateTime.parse(datetimeEndStr).atZoneSameInstant(systemZone)
                        val endDate = odet.toLocalDate()
                        val endTime = odet.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

                        Task(
                            id = raw.id,
                            nameService = raw.nameService,
                            nameCustomer = raw.nameCustomer,
                            phoneNumber = raw.phoneNumber,
                            location = raw.location,
                            startDate = startDate,
                            endDate = endDate,
                            startTime = startTime,
                            endTime = endTime,
                            status = raw.status,
                            description = raw.description,
                            customPrice = raw.customPrice,
                        )
                    } catch (e: Exception) {
                        Log.e("Calendar", "Error parsing time: $datetimeStartStr", e)
                        null
                    }
                }

                _tasks.value = tasksList
            } catch (e: Exception) {
                Log.e("Calendar", "Failed to fetch tasks", e)
            }
        }
    }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        val string = value.format(formatter)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        val string = decoder.decodeString()
        return LocalDate.parse(string, formatter)
    }
}
