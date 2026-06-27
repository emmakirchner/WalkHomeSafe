package com.example.walkhomesafe.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.services.permissions.PermissionIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that manages runtime permission requests.
 * Provides methods to request individual permissions and handles the grant/deny callbacks.
 *
 * @property permissionRequests SharedFlow emitting permission intents for the UI to handle
 */
class PermissionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    private val _permissionRequests =
        MutableSharedFlow<PermissionIntent>(
            extraBufferCapacity = 4
        )

    val permissionRequests = _permissionRequests.asSharedFlow()

    private var pendingAction: (() -> Unit)? = null
    private var pendingDenied: (() -> Unit)? = null

    /**
     * Returns the list of permissions that have not yet been granted
     * and should be requested at app startup.
     *
     * @return List of PermissionIntents for ungranted permissions
     */
    fun getPendingStartupPermissions(): List<PermissionIntent> {
        val pending = mutableListOf<PermissionIntent>()
        if (!hasPermission(Manifest.permission.READ_CONTACTS))
            pending.add(PermissionIntent.ReadContacts)
        if (!hasPermission(Manifest.permission.SEND_SMS))
            pending.add(PermissionIntent.SendSms)
        if (Build.VERSION.SDK_INT >= 33 &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
            pending.add(PermissionIntent.Notifications)
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            pending.add(PermissionIntent.AccessFineLocation)
        return pending
    }

    /**
     * Called when a permission request result is available.
     * Invokes the pending action if granted, or pending denied callback if denied.
     *
     * @param granted true if the permission was granted, false otherwise
     */
    fun onPermissionResult(granted: Boolean) {
        val action = pendingAction
        val denied = pendingDenied
        pendingAction = null
        pendingDenied = null
        if (granted) {
            action?.invoke()
        } else {
            denied?.invoke()
        }
    }

    /**
     * Requests the SEND_SMS permission. Invokes the callback immediately if already granted.
     *
     * @param onGranted Callback invoked when the permission is granted
     */
    fun requestSendSms(onGranted: () -> Unit) {
        if (hasPermission(Manifest.permission.SEND_SMS)) {
            onGranted()
        } else {
            pendingAction = onGranted
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.SendSms)
            }
        }
    }

    /**
     * Requests SEND_SMS first, then POST_NOTIFICATIONS (Android 13+).
     * Invokes the callback only after all permissions are granted.
     *
     * @param onGranted Callback invoked when all requested permissions are granted
     */
    fun requestSendSmsAndNotifications(onGranted: () -> Unit) {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            pendingAction = {
                requestSendSmsAndNotifications(onGranted)
            }
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.SendSms)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pendingAction = onGranted
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.Notifications)
            }
            return
        }

        onGranted()
        pendingAction = null
    }

    /**
     * Requests the POST_NOTIFICATIONS permission (Android 13+).
     * Invokes the callback immediately if already granted or below API 33.
     *
     * @param onGranted Callback invoked when the permission is granted
     */
    fun requestPostNotifications(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33 ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            onGranted()
        } else {
            pendingAction = onGranted
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.Notifications)
            }
        }
    }

    /**
     * Requests the READ_CONTACTS permission. Invokes the callback immediately if already granted.
     *
     * @param onGranted Callback invoked when the permission is granted
     */
    fun requestReadContacts(onGranted: () -> Unit) {
        if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            onGranted()
        } else {
            pendingAction = onGranted
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.ReadContacts)
            }
        }
    }

    /**
     * Requests the ACCESS_FINE_LOCATION permission.
     * Supports both onGranted and onDenied callbacks.
     *
     * @param onDenied Optional callback invoked when the permission is denied
     * @param onGranted Callback invoked when the permission is granted
     */
    fun requestAccessFineLocation(
        onDenied: (() -> Unit)? = null,
        onGranted: () -> Unit
    ) {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onGranted()
        } else {
            pendingAction = onGranted
            pendingDenied = onDenied
            viewModelScope.launch {
                _permissionRequests.emit(PermissionIntent.AccessFineLocation)
            }
        }
    }

    /**
     * Checks whether the ACCESS_FINE_LOCATION permission has been granted.
     *
     * @return true if the permission is granted, false otherwise
     */
    fun hasFineLocationPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Checks whether a specific Android permission has been granted.
     *
     * @param permission The Android permission string to check
     * @return true if the permission is granted, false otherwise
     */
    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}
