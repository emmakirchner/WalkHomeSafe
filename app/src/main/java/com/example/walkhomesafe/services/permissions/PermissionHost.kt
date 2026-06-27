package com.example.walkhomesafe.services.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/**
 * Composable that manages runtime permission requests via ActivityResultContracts.
 * Handles both initial startup permission batch and subsequent individual requests via a Flow.
 *
 * @param permissionFlow Flow of individual permission requests at runtime
 * @param startupPermissions List of permissions to request on initial composition
 * @param onResult Callback with the grant result (true = granted, false = denied)
 */
@Composable
fun PermissionHost(
    permissionFlow: Flow<PermissionIntent>,
    startupPermissions: List<PermissionIntent>,
    onResult: (granted: Boolean) -> Unit
) {
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        results.forEach { (_, granted) ->
            onResult(granted)
        }
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }

    /**
     * Maps a PermissionIntent to the corresponding Android permission string.
     *
     * @param intent The permission intent to map
     * @return The Android permission string
     */
    fun permissionString(intent: PermissionIntent): String = when (intent) {
        PermissionIntent.SendSms -> Manifest.permission.SEND_SMS
        PermissionIntent.Notifications -> Manifest.permission.POST_NOTIFICATIONS
        PermissionIntent.ReadContacts -> Manifest.permission.READ_CONTACTS
        PermissionIntent.AccessFineLocation -> Manifest.permission.ACCESS_FINE_LOCATION
    }

    /**
     * Launches the appropriate permission request dialog for the given intent.
     *
     * @param intent The permission intent to request
     */
    fun launchPermission(intent: PermissionIntent) {
        when (intent) {
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

            PermissionIntent.AccessFineLocation ->
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
        }
    }

    LaunchedEffect(startupPermissions) {
        if (startupPermissions.isNotEmpty()) {
            val perms = startupPermissions
                .map { permissionString(it) }
                .toTypedArray()
            multiplePermissionLauncher.launch(perms)
        }
    }

    LaunchedEffect(Unit) {
        permissionFlow.collect { request ->
            launchPermission(request)
        }
    }
}