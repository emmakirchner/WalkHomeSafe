package com.example.walkhomesafe.data

import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val EMERGENCY_MESSAGE = stringPreferencesKey("emergency_message")
    val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts")
}
