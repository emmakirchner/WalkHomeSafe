package com.example.walkhomesafe.services.permissions

sealed interface PermissionIntent {
    data object SendSms : PermissionIntent
    data object Notifications : PermissionIntent
    data object ReadContacts : PermissionIntent
    data object AccessFineLocation : PermissionIntent
}