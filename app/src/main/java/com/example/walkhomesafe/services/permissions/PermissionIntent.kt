package com.example.walkhomesafe.services.permissions

/**
 * Sealed interface representing the various runtime permissions that the app can request.
 */
sealed interface PermissionIntent {
    /** Request for SEND_SMS permission. */
    data object SendSms : PermissionIntent
    /** Request for POST_NOTIFICATIONS permission. */
    data object Notifications : PermissionIntent
    /** Request for READ_CONTACTS permission. */
    data object ReadContacts : PermissionIntent
    /** Request for ACCESS_FINE_LOCATION permission. */
    data object AccessFineLocation : PermissionIntent
}