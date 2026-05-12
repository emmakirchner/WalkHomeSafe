package com.example.walkhomesafe.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun PermissionHost(
    permissionFlow: Flow<PermissionIntent>,
    onGranted: (PermissionIntent) -> Unit
) {
    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) onGranted(PermissionIntent.SendSms)
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) onGranted(PermissionIntent.Notifications)
        }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) onGranted(PermissionIntent.ReadContacts)
        }

    LaunchedEffect(Unit) {
        permissionFlow.collect { request ->
            when (request) {
                PermissionIntent.SendSms ->
                    smsPermissionLauncher.launch(
                        Manifest.permission.SEND_SMS
                    )

                PermissionIntent.Notifications ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }

                PermissionIntent.ReadContacts ->
                   contactsPermissionLauncher.launch(
                       Manifest.permission.READ_CONTACTS
                   )
            }
        }
    }
}