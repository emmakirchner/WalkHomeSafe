package com.example.walkhomesafe.data

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Preference keys for the DataStore used to persist app settings and state.
 *
 * @property EMERGENCY_MESSAGE Key for the custom emergency SMS text
 * @property EMERGENCY_CONTACTS Key for the serialized list of emergency contacts
 * @property TIMER_END_TIME Key for the walk-home timer end timestamp
 * @property TIMER_DURATION Key for the walk-home timer duration in minutes
 * @property TIMER_EMERGENCY_SENT Key for the flag indicating an emergency SMS was already sent
 */
object DataStoreKeys {
    val EMERGENCY_MESSAGE = stringPreferencesKey("emergency_message")
    val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts")
    val TIMER_END_TIME = stringPreferencesKey("timer_end_time")
    val TIMER_DURATION = stringPreferencesKey("timer_duration")
    val TIMER_EMERGENCY_SENT = stringPreferencesKey("timer_emergency_sent")
}
