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
        MutableSharedFlow<PermissionIntent>()

    val permissionRequests = _permissionRequests.asSharedFlow()

    private var pendingAction: (() -> Unit)? = null
    private val pendingStartupPermissions = mutableListOf<PermissionIntent>()

    fun requestStartupPermissions() {
        pendingStartupPermissions.clear()
        if (!hasPermission(Manifest.permission.READ_CONTACTS))
            pendingStartupPermissions.add(PermissionIntent.ReadContacts)
        if (!hasPermission(Manifest.permission.SEND_SMS))
            pendingStartupPermissions.add(PermissionIntent.SendSms)
        if (Build.VERSION.SDK_INT >= 33 &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pendingStartupPermissions.add(PermissionIntent.Notifications)
        }
        requestNextStartupPermission()
    }

    private fun requestNextStartupPermission() {
        if (pendingStartupPermissions.isEmpty()) return
        val intent = pendingStartupPermissions.removeAt(0)
        viewModelScope.launch {
            _permissionRequests.emit(intent)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            pendingAction?.invoke()
        }
        pendingAction = null
        requestNextStartupPermission()
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

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}
