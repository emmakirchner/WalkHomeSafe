package com.example.walkhomesafe.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.walkhomesafe.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val DATASTORE_NAME = "walkhomesafe_preferences"
val Context.dataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

private const val DEFAULT_MESSAGE =
    "NOTFALL: Ich brauche Hilfe! Ich bin hier: [STANDORT-LINK]. Bitte schaut sofort nach mir. Dies ist eine automatische Nachricht von WalkHomeSafe."

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

fun emergencyMessageFlow(context: Context): Flow<String> =
    context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.EMERGENCY_MESSAGE] ?: DEFAULT_MESSAGE
    }





