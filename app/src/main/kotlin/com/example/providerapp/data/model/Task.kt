package com.example.providerapp.data.model
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

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


// ViewModel and fetching logic moved to ui/tasks and repository per MVVM.

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
