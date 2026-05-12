package com.example.walkhomesafe.presentation.permissions

sealed interface PermissionIntent {
    data object SendSms : PermissionIntent
    data object Notifications : PermissionIntent
    data object ReadContacts : PermissionIntent
}