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

    fun hasFineLocationPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}
