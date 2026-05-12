package com.example.walkhomesafe.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.presentation.permissions.PermissionIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PermissionsViewModel : ViewModel() {
    private val _permissionRequests =
        MutableSharedFlow<PermissionIntent>()

    val permissionRequests = _permissionRequests.asSharedFlow()

    fun onSendSmsRequested() {
        viewModelScope.launch {
            _permissionRequests.emit(PermissionIntent.SendSms)
        }
    }

    fun onAlarmRequested() {
        viewModelScope.launch {
            _permissionRequests.emit(PermissionIntent.Notifications)
        }
    }

    fun onReadContactsRequested() {
        viewModelScope.launch {
            _permissionRequests.emit(PermissionIntent.ReadContacts)
        }
    }

    fun requestStartupPermissions(context: Context) {
        viewModelScope.launch {

            if (!context.hasPermission(Manifest.permission.READ_CONTACTS)) {
                _permissionRequests.emit(PermissionIntent.ReadContacts)
            }

            if (!context.hasPermission(Manifest.permission.SEND_SMS)) {
                _permissionRequests.emit(PermissionIntent.SendSms)
            }

            if (Build.VERSION.SDK_INT >= 33 &&
                !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                _permissionRequests.emit(PermissionIntent.Notifications)
            }
        }
    }

    fun Context.hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

}