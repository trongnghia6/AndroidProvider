package com.example.providerapp.ui.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.providerapp.core.supabase
import com.example.providerapp.data.model.Task
import com.example.providerapp.data.model.TaskRaw
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class TaskViewModel() : ViewModel() {
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

                val tasksList = response.mapNotNull { raw ->
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


