package com.example.walkhomesafe.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.walkhomesafe.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val DATASTORE_NAME = "walkhomesafe_preferences"
val Context.dataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

private val jsonFormat = Json {
    ignoreUnknownKeys = true
}

suspend fun saveEmergencyContacts(
    context: Context,
    contacts: List<EmergencyContact>
) {
    val json = jsonFormat.encodeToString(contacts)

    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.EMERGENCY_CONTACTS] = json
    }
}

fun emergencyContactsFlow(context: Context): Flow<List<EmergencyContact>> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.EMERGENCY_CONTACTS]?.let { json ->
            jsonFormat.decodeFromString(json)
        } ?: emptyList()
    }

suspend fun saveEmergencyMessage(
    context: Context,
    message: String
) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.EMERGENCY_MESSAGE] = message
    }
}

fun emergencyMessageFlow(context: Context): Flow<String?> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.EMERGENCY_MESSAGE]
    }

suspend fun saveTimerEndTime(context: Context, endTime: Long) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME] = endTime.toString()
    }
}

fun timerEndTimeFlow(context: Context): Flow<Long> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME]?.toLongOrNull() ?: 0L
    }

suspend fun loadTimerEndTime(context: Context): Long {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME]?.toLongOrNull() ?: 0L
    }.first()
}

suspend fun saveTimerDuration(context: Context, duration: Int) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION] = duration.toString()
    }
}

fun timerDurationFlow(context: Context): Flow<Int> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION]?.toIntOrNull() ?: 0
    }

suspend fun loadTimerDuration(context: Context): Int {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION]?.toIntOrNull() ?: 0
    }.first()
}

suspend fun saveTimerEmergencySent(context: Context, sent: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_EMERGENCY_SENT] = sent.toString()
    }
}

suspend fun loadTimerEmergencySent(context: Context): Boolean {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_EMERGENCY_SENT]?.toBoolean() ?: false
    }.first()
}

suspend fun clearTimerData(context: Context) {
    context.dataStore.edit { prefs ->
        prefs.remove(DataStoreKeys.TIMER_END_TIME)
        prefs.remove(DataStoreKeys.TIMER_DURATION)
        prefs.remove(DataStoreKeys.TIMER_EMERGENCY_SENT)
    }
}


