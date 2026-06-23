package com.example.walkhomesafe.data

import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val EMERGENCY_MESSAGE = stringPreferencesKey("emergency_message")
    val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts")
    val TIMER_END_TIME = stringPreferencesKey("timer_end_time")
    val TIMER_DURATION = stringPreferencesKey("timer_duration")
    val TIMER_EMERGENCY_SENT = stringPreferencesKey("timer_emergency_sent")
}
