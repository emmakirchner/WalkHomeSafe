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

/**
 * DataStore instance for persisting walkhomesafe preferences.
 */
val Context.dataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

private val jsonFormat = Json {
    ignoreUnknownKeys = true
}

/**
 * Persists the list of emergency contacts as JSON in DataStore.
 *
 * @param context Application context for DataStore access
 * @param contacts The list of emergency contacts to save
 */
suspend fun saveEmergencyContacts(
    context: Context,
    contacts: List<EmergencyContact>
) {
    val json = jsonFormat.encodeToString(contacts)

    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.EMERGENCY_CONTACTS] = json
    }
}

/**
 * Returns a Flow of the persisted emergency contact list.
 *
 * @param context Application context for DataStore access
 * @return Flow emitting the current contact list
 */
fun emergencyContactsFlow(context: Context): Flow<List<EmergencyContact>> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.EMERGENCY_CONTACTS]?.let { json ->
            jsonFormat.decodeFromString(json)
        } ?: emptyList()
    }

/**
 * Persists the custom emergency SMS message text in DataStore.
 *
 * @param context Application context for DataStore access
 * @param message The emergency message text to save
 */
suspend fun saveEmergencyMessage(
    context: Context,
    message: String
) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.EMERGENCY_MESSAGE] = message
    }
}

/**
 * Returns a Flow of the persisted emergency message text.
 *
 * @param context Application context for DataStore access
 * @return Flow emitting the current emergency message, or null if not set
 */
fun emergencyMessageFlow(context: Context): Flow<String?> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.EMERGENCY_MESSAGE]
    }

/**
 * Persists the walk-home timer end timestamp in DataStore.
 *
 * @param context Application context for DataStore access
 * @param endTime The end time as milliseconds since epoch
 */
suspend fun saveTimerEndTime(context: Context, endTime: Long) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME] = endTime.toString()
    }
}

/**
 * Returns a Flow of the persisted timer end timestamp.
 *
 * @param context Application context for DataStore access
 * @return Flow emitting the timer end time in milliseconds, 0 if not set
 */
fun timerEndTimeFlow(context: Context): Flow<Long> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME]?.toLongOrNull() ?: 0L
    }

/**
 * Loads the timer end timestamp directly (suspend version).
 *
 * @param context Application context for DataStore access
 * @return The timer end time in milliseconds, 0 if not set
 */
suspend fun loadTimerEndTime(context: Context): Long {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_END_TIME]?.toLongOrNull() ?: 0L
    }.first()
}

/**
 * Persists the walk-home timer duration in minutes.
 *
 * @param context Application context for DataStore access
 * @param duration The timer duration in minutes
 */
suspend fun saveTimerDuration(context: Context, duration: Int) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION] = duration.toString()
    }
}

/**
 * Returns a Flow of the persisted timer duration.
 *
 * @param context Application context for DataStore access
 * @return Flow emitting the timer duration in minutes, 0 if not set
 */
fun timerDurationFlow(context: Context): Flow<Int> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION]?.toIntOrNull() ?: 0
    }

/**
 * Loads the timer duration directly (suspend version).
 *
 * @param context Application context for DataStore access
 * @return The timer duration in minutes, 0 if not set
 */
suspend fun loadTimerDuration(context: Context): Int {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_DURATION]?.toIntOrNull() ?: 0
    }.first()
}

/**
 * Persists whether the emergency SMS has already been sent for the current timer.
 *
 * @param context Application context for DataStore access
 * @param sent true if emergency SMS was sent, false otherwise
 */
suspend fun saveTimerEmergencySent(context: Context, sent: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[DataStoreKeys.TIMER_EMERGENCY_SENT] = sent.toString()
    }
}

/**
 * Loads whether the emergency SMS was already sent (suspend version).
 *
 * @param context Application context for DataStore access
 * @return true if emergency SMS was already sent, false otherwise
 */
suspend fun loadTimerEmergencySent(context: Context): Boolean {
    return context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.TIMER_EMERGENCY_SENT]?.toBoolean() ?: false
    }.first()
}

/**
 * Removes all timer-related entries (end time, duration, emergency sent flag) from DataStore.
 *
 * @param context Application context for DataStore access
 */
suspend fun clearTimerData(context: Context) {
    context.dataStore.edit { prefs ->
        prefs.remove(DataStoreKeys.TIMER_END_TIME)
        prefs.remove(DataStoreKeys.TIMER_DURATION)
        prefs.remove(DataStoreKeys.TIMER_EMERGENCY_SENT)
    }
}


